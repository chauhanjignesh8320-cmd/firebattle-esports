import crypto from 'node:crypto';

export const FIREBASE_PROJECT_ID = 'gen-lang-client-0958360846';
export const FIRESTORE_DATABASE_ID =
  'ai-studio-android-firebatt-309dafa7-3a87-4c11-b9d8-19abaebe0f47';

const FIRESTORE_ROOT = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/${FIRESTORE_DATABASE_ID}`;
const DOCS_BASE = `${FIRESTORE_ROOT}/documents`;
const CERTS_URL =
  'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';

let cachedCerts = null;
let cachedCertsExpiresAt = 0;

let cachedAdminToken = null;
let cachedAdminTokenExpiresAt = 0;

let cachedMessagingToken = null;
let cachedMessagingTokenExpiresAt = 0;

async function mintGoogleOAuthToken(scope) {
  const sa = parseServiceAccountJson();
  if (!sa || !sa.client_email || !sa.private_key) {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_JSON is not configured.');
  }

  const now = Date.now();
  const iat = Math.floor(now / 1000);
  const exp = iat + 3600;
  const header = Buffer.from(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).toString('base64url');
  const claims = Buffer.from(
    JSON.stringify({
      iss: sa.client_email,
      scope,
      aud: 'https://oauth2.googleapis.com/token',
      iat,
      exp,
    })
  ).toString('base64url');

  const signer = crypto.createSign('RSA-SHA256');
  signer.update(`${header}.${claims}`);
  signer.end();
  const signature = signer.sign(sa.private_key).toString('base64url');
  const assertion = `${header}.${claims}.${signature}`;

  const body = new URLSearchParams({
    grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
    assertion,
  });

  const resp = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  });
  const data = await resp.json();
  if (!resp.ok || !data.access_token) {
    throw new Error(`Google OAuth error: ${data.error_description || data.error || resp.status}`);
  }
  return {
    accessToken: data.access_token,
    expiresAt: now + Math.max(60, Number(data.expires_in || 3600) - 120) * 1000,
  };
}

export async function getFirestoreAdminToken() {
  const now = Date.now();
  if (cachedAdminToken && now < cachedAdminTokenExpiresAt) {
    return cachedAdminToken;
  }
  const minted = await mintGoogleOAuthToken('https://www.googleapis.com/auth/datastore');
  cachedAdminToken = minted.accessToken;
  cachedAdminTokenExpiresAt = minted.expiresAt;
  return cachedAdminToken;
}

export async function getFcmMessagingToken() {
  const now = Date.now();
  if (cachedMessagingToken && now < cachedMessagingTokenExpiresAt) {
    return cachedMessagingToken;
  }
  const minted = await mintGoogleOAuthToken('https://www.googleapis.com/auth/firebase.messaging');
  cachedMessagingToken = minted.accessToken;
  cachedMessagingTokenExpiresAt = minted.expiresAt;
  return cachedMessagingToken;
}

export async function sendFcmAdminDepositAlert({
  depositId,
  ign,
  phone,
  amount,
  utr,
}) {
  const token = await getFcmMessagingToken();
  const url = `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`;
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      message: {
        topic: 'admin_deposits',
        data: {
          type: 'deposit_request',
          depositId: String(depositId || ''),
          ign: String(ign || 'Player'),
          phone: String(phone || ''),
          amount: String(amount || 0),
          utr: String(utr || ''),
        },
        android: {
          priority: 'HIGH',
          ttl: '3600s',
        },
      },
    }),
  });
  const json = await resp.json().catch(() => ({}));
  if (!resp.ok) {
    throw new Error(json?.error?.message || `FCM send failed (${resp.status})`);
  }
  return json;
}

export function parseServiceAccountJson() {
  const raw = (process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '').trim();
  if (!raw) return null;
  try {
    const parsed = raw.startsWith('{')
      ? JSON.parse(raw)
      : JSON.parse(Buffer.from(raw, 'base64').toString('utf8'));
    if (parsed && typeof parsed.private_key === 'string') {
      parsed.private_key = parsed.private_key.replace(/\\n/g, '\n');
    }
    return parsed;
  } catch {
    return null;
  }
}

export function isFirebaseAdminConfigured() {
  const sa = parseServiceAccountJson();
  return Boolean(sa && sa.client_email && sa.private_key);
}

async function getFirebasePublicCerts() {
  const now = Date.now();
  if (cachedCerts && now < cachedCertsExpiresAt) {
    return cachedCerts;
  }
  const resp = await fetch(CERTS_URL);
  if (!resp.ok) {
    throw new Error(`Failed to fetch Firebase auth certificates (${resp.status})`);
  }
  const cacheControl = resp.headers.get('cache-control') || '';
  const maxAgeMatch = cacheControl.match(/max-age=(\d+)/i);
  const maxAgeSec = maxAgeMatch ? Number(maxAgeMatch[1]) : 3600;
  cachedCerts = await resp.json();
  cachedCertsExpiresAt = now + Math.max(60, maxAgeSec - 60) * 1000;
  return cachedCerts;
}

export async function verifyFirebaseIdToken(idToken) {
  if (!idToken || typeof idToken !== 'string') {
    throw new Error('Missing Firebase auth token.');
  }
  const parts = idToken.split('.');
  if (parts.length !== 3) {
    throw new Error('Malformed Firebase auth token.');
  }
  const [headerB64, payloadB64, sigB64] = parts;
  const header = JSON.parse(Buffer.from(headerB64, 'base64url').toString('utf8'));
  const payload = JSON.parse(Buffer.from(payloadB64, 'base64url').toString('utf8'));

  if (header.alg !== 'RS256' || !header.kid) {
    throw new Error('Unsupported auth token algorithm.');
  }

  const certs = await getFirebasePublicCerts();
  const certPem = certs[header.kid];
  if (!certPem) {
    throw new Error('Auth token signing key not recognized.');
  }

  const verifier = crypto.createVerify('RSA-SHA256');
  verifier.update(`${headerB64}.${payloadB64}`);
  verifier.end();
  const sigBuf = Buffer.from(sigB64, 'base64url');
  const valid = verifier.verify(certPem, sigBuf);
  if (!valid) {
    throw new Error('Invalid Firebase auth token signature.');
  }

  const nowSec = Math.floor(Date.now() / 1000);
  if (payload.aud !== FIREBASE_PROJECT_ID) {
    throw new Error('Firebase auth token audience mismatch.');
  }
  if (payload.iss !== `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`) {
    throw new Error('Firebase auth token issuer mismatch.');
  }
  if (typeof payload.exp !== 'number' || payload.exp <= nowSec) {
    throw new Error('Firebase auth token expired.');
  }
  if (typeof payload.sub !== 'string' || !payload.sub.trim()) {
    throw new Error('Firebase auth token missing subject.');
  }
  return {
    uid: payload.sub.trim(),
    email: typeof payload.email === 'string' ? payload.email : '',
  };
}

export function decodeFirestoreFields(fields = {}) {
  const out = {};
  for (const [k, v] of Object.entries(fields)) {
    if (!v || typeof v !== 'object') continue;
    if ('stringValue' in v) out[k] = v.stringValue;
    else if ('integerValue' in v) out[k] = Number(v.integerValue);
    else if ('doubleValue' in v) out[k] = Number(v.doubleValue);
    else if ('booleanValue' in v) out[k] = Boolean(v.booleanValue);
    else if ('timestampValue' in v) out[k] = v.timestampValue;
    else if ('nullValue' in v) out[k] = null;
  }
  return out;
}

function docResourceName(docPath) {
  return `projects/${FIREBASE_PROJECT_ID}/databases/${FIRESTORE_DATABASE_ID}/documents/${docPath}`;
}

export async function fsGetDoc(docPath, accessToken) {
  const resp = await fetch(`${DOCS_BASE}/${docPath}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (resp.status === 404) {
    return { exists: false, data: null, updateTime: null, name: docResourceName(docPath) };
  }
  const json = await resp.json();
  if (!resp.ok) {
    throw new Error(json?.error?.message || `Firestore get failed (${resp.status})`);
  }
  return {
    exists: true,
    name: json.name,
    updateTime: json.updateTime,
    data: decodeFirestoreFields(json.fields),
  };
}

export async function fsCreatePendingDeposit({
  orderId,
  userId,
  ign,
  phone,
  amount,
  bonusAmount,
  accessToken,
}) {
  const nowIso = new Date().toISOString();
  const payload = {
    fields: {
      userId: { stringValue: userId },
      ign: { stringValue: ign },
      phone: { stringValue: phone },
      amount: { integerValue: String(amount) },
      bonusAmount: { integerValue: String(bonusAmount) },
      utr: { stringValue: orderId },
      status: { stringValue: 'Pending' },
      paymentGateway: { stringValue: 'PHONEPE_PG' },
      createdAt: { timestampValue: nowIso },
      updatedAt: { timestampValue: nowIso },
    },
  };

  const url = `${DOCS_BASE}/deposits?documentId=${encodeURIComponent(orderId)}`;
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  const json = await resp.json();
  if (!resp.ok) {
    throw new Error(json?.error?.message || `Failed to create pending deposit (${resp.status})`);
  }
  return json;
}

export async function fsDeleteDoc(docPath, accessToken) {
  await fetch(`${DOCS_BASE}/${docPath}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export async function fsAtomicSettleDeposit({
  orderId,
  targetStatus, // 'Approved' | 'Rejected'
  realUtr,
  accessToken,
}) {
  const beginResp = await fetch(`${DOCS_BASE}:beginTransaction`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ options: { readWrite: {} } }),
  });
  const beginJson = await beginResp.json();
  if (!beginResp.ok || !beginJson.transaction) {
    throw new Error(beginJson?.error?.message || 'Failed to begin Firestore transaction.');
  }
  const transaction = beginJson.transaction;

  const rollbackQuietly = async () => {
    try {
      await fetch(`${DOCS_BASE}:rollback`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ transaction }),
      });
    } catch {
      // ignore rollback failure
    }
  };

  try {
    const depName = docResourceName(`deposits/${orderId}`);
    const depGetResp = await fetch(`${DOCS_BASE}:batchGet`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ documents: [depName], transaction }),
    });
    const depBatch = await depGetResp.json();
    const depItem = Array.isArray(depBatch) ? depBatch[0] : null;
    if (!depItem?.found) {
      await rollbackQuietly();
      return { found: false, alreadyProcessed: false };
    }

    const depData = decodeFirestoreFields(depItem.found.fields);
    if (depData.status !== 'Pending') {
      await rollbackQuietly();
      return {
        found: true,
        alreadyProcessed: true,
        status: depData.status,
        amount: Number(depData.amount || 0),
        bonusAmount: Number(depData.bonusAmount || 0),
        utr: String(depData.utr || orderId),
      };
    }

    const nowIso = new Date().toISOString();
    const writes = [];

    if (targetStatus === 'Rejected') {
      writes.push({
        update: {
          name: depName,
          fields: {
            status: { stringValue: 'Rejected' },
            bonusAmount: { integerValue: '0' },
            updatedAt: { timestampValue: nowIso },
          },
        },
        updateMask: { fieldPaths: ['status', 'bonusAmount', 'updatedAt'] },
        currentDocument: { updateTime: depItem.found.updateTime },
      });
    } else {
      const userId = String(depData.userId || '');
      const userName = docResourceName(`users/${userId}`);
      const userGetResp = await fetch(`${DOCS_BASE}:batchGet`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ documents: [userName], transaction }),
      });
      const userBatch = await userGetResp.json();
      const userItem = Array.isArray(userBatch) ? userBatch[0] : null;
      if (!userItem?.found) {
        await rollbackQuietly();
        throw new Error('Player wallet document not found.');
      }
      const userData = decodeFirestoreFields(userItem.found.fields);
      const currentWallet = Number(userData.wallet || 0);
      const amount = Number(depData.amount || 0);
      const bonusAmount = Number(depData.bonusAmount || 0);
      const nextWallet = Math.min(currentWallet + amount + bonusAmount, 10_000_000);
      const cleanUtr = String(realUtr || orderId).trim().slice(0, 32) || orderId;

      writes.push({
        update: {
          name: depName,
          fields: {
            status: { stringValue: 'Approved' },
            utr: { stringValue: cleanUtr },
            updatedAt: { timestampValue: nowIso },
          },
        },
        updateMask: { fieldPaths: ['status', 'utr', 'updatedAt'] },
        currentDocument: { updateTime: depItem.found.updateTime },
      });
      writes.push({
        update: {
          name: userName,
          fields: {
            wallet: { integerValue: String(nextWallet) },
            updatedAt: { timestampValue: nowIso },
          },
        },
        updateMask: { fieldPaths: ['wallet', 'updatedAt'] },
        currentDocument: { updateTime: userItem.found.updateTime },
      });
    }

    const commitResp = await fetch(`${DOCS_BASE}:commit`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ transaction, writes }),
    });
    const commitJson = await commitResp.json();
    if (!commitResp.ok) {
      throw new Error(commitJson?.error?.message || `Firestore commit failed (${commitResp.status})`);
    }

    return {
      found: true,
      alreadyProcessed: false,
      status: targetStatus,
      amount: Number(depData.amount || 0),
      bonusAmount: targetStatus === 'Approved' ? Number(depData.bonusAmount || 0) : 0,
      utr: targetStatus === 'Approved' ? String(realUtr || orderId) : String(depData.utr || orderId),
    };
  } catch (err) {
    await rollbackQuietly();
    throw err;
  }
}
