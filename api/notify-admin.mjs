import {
  fsGetDoc,
  getFirestoreAdminToken,
  isFirebaseAdminConfigured,
  sendFcmAdminDepositAlert,
  verifyFirebaseIdToken,
} from './_lib/firebaseAdminRest.mjs';

const DOC_ID_REGEX = /^[a-zA-Z0-9_-]{6,128}$/;

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, message: 'Method not allowed' });
  }

  if (!isFirebaseAdminConfigured()) {
    return res.status(200).json({ ok: false, skipped: true, reason: 'service_account_missing' });
  }

  try {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    const authUser = await verifyFirebaseIdToken(token);

    const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    const depositId = String(body.depositId || '').trim();
    if (!DOC_ID_REGEX.test(depositId)) {
      return res.status(400).json({ ok: false, message: 'Invalid deposit ID.' });
    }

    const adminToken = await getFirestoreAdminToken();
    const depDoc = await fsGetDoc(`deposits/${depositId}`, adminToken);
    if (!depDoc.exists || !depDoc.data) {
      return res.status(404).json({ ok: false, message: 'Deposit record not found.' });
    }
    if (String(depDoc.data.userId || '') !== authUser.uid) {
      return res.status(403).json({ ok: false, message: 'Deposit does not belong to caller.' });
    }
    if (depDoc.data.status !== 'Pending') {
      return res.status(200).json({ ok: true, skipped: true });
    }

    await sendFcmAdminDepositAlert({
      depositId,
      ign: depDoc.data.ign,
      phone: depDoc.data.phone,
      amount: depDoc.data.amount,
      utr: depDoc.data.utr,
    });

    return res.status(200).json({ ok: true });
  } catch (err) {
    const msg = err?.message || 'Notification dispatch failed.';
    const isAuthErr = /token|signature|expired|audience|issuer|subject/i.test(msg);
    return res.status(isAuthErr ? 401 : 500).json({ ok: false, message: msg });
  }
}
