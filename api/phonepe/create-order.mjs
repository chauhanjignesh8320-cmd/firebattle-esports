import crypto from 'node:crypto';
import {
  fsCreatePendingDeposit,
  fsDeleteDoc,
  fsGetDoc,
  getFirestoreAdminToken,
  isFirebaseAdminConfigured,
  verifyFirebaseIdToken,
} from '../_lib/firebaseAdminRest.mjs';
import { createPhonePeCheckoutOrder, isPhonePeConfigured } from '../_lib/phonepeClient.mjs';

const RETURN_BASE_URL = 'https://firebetal.com/phonepe-return';

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, message: 'Method not allowed' });
  }

  if (!isPhonePeConfigured() || !isFirebaseAdminConfigured()) {
    return res.status(503).json({
      ok: false,
      notConfigured: true,
      message: 'PhonePe Payment Gateway credentials are not configured on the server yet.',
    });
  }

  try {
    const authHeader = req.headers.authorization || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    const authUser = await verifyFirebaseIdToken(token);

    const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    const amount = Number(body.amount);
    if (!Number.isInteger(amount) || amount < 1 || amount > 100000) {
      return res.status(400).json({ ok: false, message: 'Amount must be between ₹1 and ₹1,00,000.' });
    }

    const adminToken = await getFirestoreAdminToken();
    const [userDoc, settingsDoc] = await Promise.all([
      fsGetDoc(`users/${authUser.uid}`, adminToken),
      fsGetDoc('settings/app', adminToken),
    ]);

    if (!userDoc.exists || !userDoc.data) {
      return res.status(400).json({ ok: false, message: 'Complete your player profile before adding money.' });
    }

    const ign = String(userDoc.data.ign || '').trim();
    const phone = String(userDoc.data.phone || '').trim();
    if (ign.length < 2 || !/^[0-9]{10}$/.test(phone)) {
      return res.status(400).json({ ok: false, message: 'Player profile details are incomplete.' });
    }

    const bonusPct = Number(settingsDoc.data?.depositBonusPercent || 0);
    const minOfferAmt = Number(settingsDoc.data?.depositOfferMinAmount || 0);
    let bonusAmount = 0;
    if (bonusPct > 0 && bonusPct <= 100 && amount >= minOfferAmt) {
      bonusAmount = Math.min(Math.floor((amount * bonusPct) / 100), amount);
    }

    const uidPart = authUser.uid.replace(/[^a-zA-Z0-9]/g, '').slice(0, 8) || 'PLAYER';
    const timePart = Date.now().toString(36).toUpperCase();
    const randPart = crypto.randomBytes(2).toString('hex').toUpperCase();
    const merchantOrderId = `FB_${uidPart}_${timePart}_${randPart}`.slice(0, 60);

    await fsCreatePendingDeposit({
      orderId: merchantOrderId,
      userId: authUser.uid,
      ign,
      phone,
      amount,
      bonusAmount,
      accessToken: adminToken,
    });

    const redirectUrl = `${RETURN_BASE_URL}?orderId=${encodeURIComponent(merchantOrderId)}`;
    let pgOrder;
    try {
      pgOrder = await createPhonePeCheckoutOrder({
        merchantOrderId,
        amountInr: amount,
        redirectUrl,
        uid: authUser.uid,
        phone,
      });
    } catch (pgErr) {
      await fsDeleteDoc(`deposits/${merchantOrderId}`, adminToken).catch(() => {});
      throw pgErr;
    }

    return res.status(200).json({
      ok: true,
      merchantOrderId,
      orderId: pgOrder.orderId,
      redirectUrl: pgOrder.redirectUrl,
      bonusAmount,
    });
  } catch (err) {
    const msg = err?.message || 'Unable to initiate PhonePe payment.';
    const isAuthErr = /token|signature|expired|audience|issuer|subject/i.test(msg);
    return res.status(isAuthErr ? 401 : 500).json({ ok: false, message: msg });
  }
}
