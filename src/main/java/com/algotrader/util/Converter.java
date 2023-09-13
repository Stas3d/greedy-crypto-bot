package com.algotrader.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Converter {

    private static final String PATTERN = "0.00";

    public static String convertToStringDecimal(double enterPrice) {
        //System.out.println("*** convertToStringDecimal - before : " + enterPrice);
        DecimalFormat df = new DecimalFormat(PATTERN);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        //System.out.println("*** Current price : " + df.format(enterPrice));
        return df.format(enterPrice);
    }

    public static double convertStringToDouble(String price) {
        return Double.parseDouble(price);
    }
}
