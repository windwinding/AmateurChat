package com.coinomi.wallet.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.coinomi.wallet.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

/**
 * @author John L. Jegutanis
 */
public class QrUtils {
    private final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    private static final Logger log = LoggerFactory.getLogger(QrUtils.class);
    private static final ErrorCorrectionLevel ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.M;

    private static final int DARK_COLOR = 0xdd000000;
    private static final int LIGHT_COLOR = 0;

    public static boolean setQr(final ImageView view, final Resources res, final String content) {
        return setQr(view, res, content, R.dimen.qr_code_size, R.dimen.qr_code_quite_zone_pixels);
    }

    priva