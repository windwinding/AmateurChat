
package com.coinomi.core.util;

import com.google.common.collect.ImmutableMap;

/**
 * @author John L. Jegutanis
 */
public class Currencies {
    public static final ImmutableMap<String, String> CURRENCY_NAMES;

    static {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("AED", "United Arab Emirates Dirham");
        builder.put("AFN", "Afghan Afghani");
        builder.put("ALL", "Albanian Lek");
        builder.put("AMD", "Armenian Dram");
        builder.put("ANG", "Netherlands Antillean Guilder");
        builder.put("AOA", "Angolan Kwanza");
        builder.put("ARS", "Argentine Peso");
        builder.put("AUD", "Australian Dollar");
        builder.put("AWG", "Aruban Florin");
        builder.put("AZN", "Azerbaijani Manat");
        builder.put("BAM", "Bosnia-Herzegovina Convertible Mark");
        builder.put("BBD", "Barbadian Dollar");
        builder.put("BDT", "Bangladeshi Taka");
        builder.put("BGN", "Bulgarian Lev");
        builder.put("BHD", "Bahraini Dinar");
        builder.put("BIF", "Burundian Franc");
        builder.put("BMD", "Bermudan Dollar");
        builder.put("BND", "Brunei Dollar");
        builder.put("BOB", "Bolivian Boliviano");
        builder.put("BRL", "Brazilian Real");
        builder.put("BSD", "Bahamian Dollar");
        builder.put("BTN", "Bhutanese Ngultrum");
        builder.put("BWP", "Botswanan Pula");
        builder.put("BYR", "Belarusian Ruble");
        builder.put("BZD", "Belize Dollar");
        builder.put("CAD", "Canadian Dollar");
        builder.put("CDF", "Congolese Franc");
        builder.put("CHF", "Swiss Franc");
        builder.put("CLF", "Chilean Unit of Account (UF)");
        builder.put("CLP", "Chilean Peso");
        builder.put("CNY", "Chinese Yuan");