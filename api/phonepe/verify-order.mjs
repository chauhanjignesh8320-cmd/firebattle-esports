import {
  fsAtomicSettleDeposit,
  fsGetDoc,
  getFirestoreAdminToken,
  isFirebaseAdminConfigured,
  verifyFirebaseIdToken,
} from '../_lib/firebaseAdminRest.mjs';
import { fetchPhonePeOrderStatus, isPhonePeConfigured } from '../_lib/phonepeClient.mjs';

const ORDER_ID_REGEX = /^[a-zA-Z0-9_-]{6,63}$/;

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, message: 'Method not allowed' });
  }

  if (!isPhonePeConfigured() || !isFirebaseAdminConfigured()) {
    return res.status(503).json({
      ok: false,
      notConfigured: true,
      message: 'PhonePe PG is not configured on the server.',
    });
  }

  try {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    const authUser = await verifyFirebaseIdToken(token);

    const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    const merchantOrderId = String(body.merchantOrderId || '').trim();
    if (!ORDER_ID_REGEX.test(merchantOrderId)) {
      return res.status(400).json({ ok: false, message: 'Invalid order reference.' });
    }

    const adminToken = await getFirestoreAdminToken();
    const depDoc = await fsGetDoc(`deposits/${merchantOrderId}`, adminToken);
    if (!depDoc.exists || !depDoc.data) {
      return res.status(404).json({ ok: false, message: 'Deposit order not found.' });
    }

    if (String(depDoc.data.userId || '') !== authUser.uid) {
      return res.status(403).json({ ok: false, message: 'Order does not belong to this player.' });
    }

    if (depDoc.data.status !== 'Pending') {
      const state = depDoc.data.status === 'Approved' ? 'COMPLETED' : 'FAILED';
      return res.status(200).json({
        ok: true,
        state,
        alreadyProcessed: true,
        amount: Number(depDoc.data.amount || 0),
        bonusAmount: depDoc.data.status === 'Approved' ? Number(depDoc.data.bonusAmount || 0) : 0,
        utr: String(depDoc.data.utr || merchantOrderId),
      });
    }

    const pgStatus = await fetchPhonePeOrderStatus(merchantOrderId);
    const expectedPaisa = Number(depDoc.data.amount || 0) * 100;

    if (pgStatus.state === 'COMPLETED') {
      if (pgStatus.amountPaisa !== expectedPaisa) {
        return res.status(400).json({
          ok: false,
          state: 'FAILED',
          message: 'Paid amount mismatch detected during verification.',
        });
      }
      const settled = await fsAtomicSettleDeposit({
        orderId: merchantOrderId,
        targetStatus: 'Approved',
        realUtr: pgStatus.utr,
        accessToken: adminToken,
      });
      return res.status(200).json({
        ok: true,
        state: 'COMPLETED',
        alreadyProcessed: settled.alreadyProcessed,
        amount: settled.amount,
        bonusAmount: settled.bonusAmount,
        utr: settled.utr,
      });
    }

    if (pgStatus.state === 'FAILED') {
      await fsAtomicSettleDeposit({
        orderId: merchantOrderId,
        targetStatus: 'Rejected',
        realUtr: merchantOrderId,
        accessToken: adminToken,
      });
      return res.status(200).json({
        ok: true,
        state: 'FAILED',
        amount: Number(depDoc.data.amount || 0),
        bonusAmount: 0,
        utr: merchantOrderId,
        message: pgStatus.errorCode ? `Payment failed (${pgStatus.errorCode})` : 'Payment failed or cancelled.',
      });
    }

    return res.status(200).json({
      ok: true,
      state: 'PENDING',
      amount: Number(depDoc.data.amount || 0),
      bonusAmount: 0,
      utr: merchantOrderId,
    });
  } catch (err) {
    const msg = err?.message || 'Unable to verify PhonePe payment.';
    const isAuthErr = /token|signature|expired|audience|issuer|subject/i.test(msg);
    return res.status(isAuthErr ? 401 : 500).json({ ok: false, message: msg });
  }
}
