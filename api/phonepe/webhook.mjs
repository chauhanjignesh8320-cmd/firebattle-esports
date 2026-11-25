import crypto from 'node:crypto';
import {
  fsAtomicSettleDeposit,
  fsGetDoc,
  getFirestoreAdminToken,
  isFirebaseAdminConfigured,
} from '../_lib/firebaseAdminRest.mjs';
import { fetchPhonePeOrderStatus, isPhonePeConfigured } from '../_lib/phonepeClient.mjs';

const ORDER_ID_REGEX = /^[a-zA-Z0-9_-]{6,63}$/;

function verifyWebhookBasicAuth(authHeader) {
  const user = (process.env.PHONEPE_WEBHOOK_USER || '').trim();
  const pass = (process.env.PHONEPE_WEBHOOK_PASS || '').trim();
  if (!user || !pass) return true; // No dashboard basic auth configured; still S2S verified below
  if (!authHeader) return false;
  const expected = crypto.createHash('sha256').update(`${user}:${pass}`).digest('hex');
  const provided = authHeader.replace(/^SHA256[:\s]+/i, '').trim().toLowerCase();
  if (expected.length !== provided.length) return false;
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(provided));
}

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, message: 'Method not allowed' });
  }

  if (!isPhonePeConfigured() || !isFirebaseAdminConfigured()) {
    return res.status(503).json({ ok: false, message: 'Server not configured.' });
  }

  if (!verifyWebhookBasicAuth(req.headers.authorization || '')) {
    return res.status(401).json({ ok: false, message: 'Unauthorized webhook caller.' });
  }

  try {
    const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    const payload = body.payload || body;
    const merchantOrderId = String(
      payload.merchantOrderId || payload.originalMerchantOrderId || ''
    ).trim();

    if (!ORDER_ID_REGEX.test(merchantOrderId)) {
      return res.status(400).json({ ok: false, message: 'Missing valid merchantOrderId.' });
    }

    const adminToken = await getFirestoreAdminToken();
    const depDoc = await fsGetDoc(`deposits/${merchantOrderId}`, adminToken);
    if (!depDoc.exists || !depDoc.data) {
      return res.status(200).json({ ok: true, ignored: true });
    }
    if (depDoc.data.status !== 'Pending') {
      return res.status(200).json({ ok: true, alreadyProcessed: true });
    }

    // Always cross-verify directly with PhonePe Status API before crediting wallet
    const pgStatus = await fetchPhonePeOrderStatus(merchantOrderId);
    const expectedPaisa = Number(depDoc.data.amount || 0) * 100;

    if (pgStatus.state === 'COMPLETED' && pgStatus.amountPaisa === expectedPaisa) {
      await fsAtomicSettleDeposit({
        orderId: merchantOrderId,
        targetStatus: 'Approved',
        realUtr: pgStatus.utr,
        accessToken: adminToken,
      });
    } else if (pgStatus.state === 'FAILED') {
      await fsAtomicSettleDeposit({
        orderId: merchantOrderId,
        targetStatus: 'Rejected',
        realUtr: merchantOrderId,
        accessToken: adminToken,
      });
    }

    return res.status(200).json({ ok: true, state: pgStatus.state });
  } catch (err) {
    return res.status(500).json({ ok: false, message: err?.message || 'Webhook error' });
  }
}
