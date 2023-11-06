package com.algotrader.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Converter {

    private static final String FORMAT_PATTERN = "0.00";

    public static String convertToStringDecimal(double enterPrice) {

        var df = new DecimalFormat(FORMAT_PATTERN);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return df.format(enterPrice);
    }
}
