let cachedOAuthToken = null;
let cachedOAuthExpiresAt = 0;

function getPhonePeConfig() {
  const clientId = (process.env.PHONEPE_CLIENT_ID || '').trim();
  const clientSecret = (process.env.PHONEPE_CLIENT_SECRET || '').trim();
  const clientVersion = (process.env.PHONEPE_CLIENT_VERSION || '1').trim();
  const env = (process.env.PHONEPE_ENV || 'production').trim().toLowerCase();
  const isSandbox = env === 'sandbox' || env === 'uat' || env === 'preprod';

  return {
    clientId,
    clientSecret,
    clientVersion,
    isSandbox,
    oauthUrl: isSandbox
      ? 'https://api-preprod.phonepe.com/apis/pg-sandbox/v1/oauth/token'
      : 'https://api.phonepe.com/apis/identity-manager/v1/oauth/token',
    payUrl: isSandbox
      ? 'https://api-preprod.phonepe.com/apis/pg-sandbox/checkout/v2/pay'
      : 'https://api.phonepe.com/apis/pg/checkout/v2/pay',
    statusUrl: (merchantOrderId) =>
      isSandbox
        ? `https://api-preprod.phonepe.com/apis/pg-sandbox/checkout/v2/order/${encodeURIComponent(merchantOrderId)}/status`
        : `https://api.phonepe.com/apis/pg/checkout/v2/order/${encodeURIComponent(merchantOrderId)}/status`,
  };
}

export function isPhonePeConfigured() {
  const cfg = getPhonePeConfig();
  return Boolean(cfg.clientId && cfg.clientSecret);
}

export async function getPhonePeAccessToken() {
  const now = Date.now();
  if (cachedOAuthToken && now < cachedOAuthExpiresAt) {
    return cachedOAuthToken;
  }
  const cfg = getPhonePeConfig();
  if (!cfg.clientId || !cfg.clientSecret) {
    throw new Error('PhonePe PG credentials are not configured.');
  }

  const form = new URLSearchParams({
    client_id: cfg.clientId,
    client_version: cfg.clientVersion,
    client_secret: cfg.clientSecret,
    grant_type: 'client_credentials',
  });

  const resp = await fetch(cfg.oauthUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: form.toString(),
  });
  const data = await resp.json();
  if (!resp.ok || !data?.access_token) {
    throw new Error(data?.message || data?.error_description || `PhonePe OAuth failed (${resp.status})`);
  }

  const expiresInSec = Number(data.expires_in || 3000);
  cachedOAuthToken = data.access_token;
  cachedOAuthExpiresAt = now + Math.max(60, expiresInSec - 120) * 1000;
  return cachedOAuthToken;
}

export async function createPhonePeCheckoutOrder({
  merchantOrderId,
  amountInr,
  redirectUrl,
  uid,
  phone,
}) {
  const cfg = getPhonePeConfig();
  const token = await getPhonePeAccessToken();
  const payload = {
    merchantOrderId,
    amount: Math.round(amountInr * 100),
    expireAfter: 1200,
    paymentFlow: {
      type: 'PG_CHECKOUT',
      message: `Firebattle Wallet Add ₹${amountInr}`,
      merchantUrls: {
        redirectUrl,
      },
    },
    metaInfo: {
      udf1: uid,
      udf2: phone,
    },
  };

  const resp = await fetch(cfg.payUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `O-Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  const data = await resp.json();
  if (!resp.ok || !data?.redirectUrl) {
    throw new Error(data?.message || data?.code || `PhonePe pay init failed (${resp.status})`);
  }
  return {
    orderId: data.orderId || merchantOrderId,
    state: data.state || 'PENDING',
    redirectUrl: data.redirectUrl,
  };
}

export async function fetchPhonePeOrderStatus(merchantOrderId) {
  const cfg = getPhonePeConfig();
  const token = await getPhonePeAccessToken();
  const resp = await fetch(cfg.statusUrl(merchantOrderId), {
    method: 'GET',
    headers: {
      Authorization: `O-Bearer ${token}`,
    },
  });
  const data = await resp.json();
  if (!resp.ok || !data?.state) {
    throw new Error(data?.message || data?.code || `PhonePe status check failed (${resp.status})`);
  }
  const firstDetail = Array.isArray(data.paymentDetails) ? data.paymentDetails[0] : null;
  const utr =
    firstDetail?.rail?.utr ||
    firstDetail?.rail?.upiTransactionId ||
    firstDetail?.transactionId ||
    data.orderId ||
    merchantOrderId;

  return {
    state: String(data.state).toUpperCase(), // COMPLETED | FAILED | PENDING
    amountPaisa: Number(data.amount || 0),
    utr: String(utr).replace(/[^a-zA-Z0-9_-]/g, '').slice(0, 32) || merchantOrderId,
    errorCode: data.errorCode || firstDetail?.errorCode || '',
  };
}
