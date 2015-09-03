package com.overmc.overpermissions.internal.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.overmc.overpermissions.exceptions.TimeFormatException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtils {// TODO make less complicated, probably via StreamTokenizer
    private static final Pattern TIME_VALIDITY_PATTERN = Pattern.compile("^((?:\\d*\\.?\\d+)(?i)(?:" + Joiner.on('|').join(CalendarUnit.ALL_ALIASES) + "))*$"); // This pattern checks for validity in input, as it matches the whole string.
    private static final Pattern TIME_PATTERN = Pattern.compile("((\\d*\\.?\\d+)(?i)(" + Joiner.on('|').join(CalendarUnit.ALL_ALIASES) + "))"); // #(.#) + a time quantifier

    private static final Comparator<String> STRING_LENGTH_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s2.length() - s1.length();
        }
    };

    private TimeUtils( ) {
    }

    /**
     * @param input the input string to be parsed, for example: "5h55m", "2.5h", "999999d"
     * @return the millisecond equivalent of all times added.
     * 
     * @throws TimeFormatException if the string isn't a valid time string, or is bigger than {@link Long#MAX_VALUE}
     */
    public static long parseMilliseconds(String input) throws TimeFormatException {
        if (!TIME_VALIDITY_PATTERN.matcher(input).matches()) {
            throw new TimeFormatException("Invalid time string: " + input);
        }
        Matcher timeSubmatcher = TIME_PATTERN.matcher(input);
        BigInteger ret = BigInteger.ZERO;
        while (timeSubmatcher.find()) {
            String numberPortion = timeSubmatcher.group(2);
            String aliasPortion = timeSubmatcher.group(3);
            if (CalendarUnit.ALIAS_MAP.containsKey(aliasPortion)) {
                CalendarUnit unit = CalendarUnit.ALIAS_MAP.get(aliasPortion);
                BigDecimal value = new BigDecimal(numberPortion);
                BigDecimal msValue = value.multiply(BigDecimal.valueOf(unit.timeInMillis));
                ret = ret.add(msValue.toBigInteger());
            } else {
                throw new TimeFormatException("The unit " + aliasPortion + " isn't valid.");
            }
        }
        long longValue = ret.longValue();
        if (ret.compareTo(BigInteger.valueOf(longValue)) == 0) {
            return ret.longValue();
        } else {
            throw new TimeFormatException("The number " + ret.toString() + " is larger than the maximum allowed.");
        }
    }

    public static String parseReadableDate(long milliseconds) {
        StringBuilder sb = new StringBuilder();
        for (int i = CalendarUnit.values().length - 1; i >= 0; i--) {
            CalendarUnit u = CalendarUnit.values()[i];
            int numUnits = (int) (milliseconds / u.timeInMillis);
            if (numUnits >= 1) {
                sb.append(", ");
                sb.append(numUnits).append(" ");
                if (numUnits >= 2) {
                    sb.append(u.mainAliasPlural); //2 minutes
                } else {
                    sb.append(u.mainAlias); //1 minute
                }
                milliseconds -= numUnits * u.timeInMillis;
            }
        }
        return sb.substring(2);
    }

    public static ImmutableList<String> getTimeUnits( ) {
        return CalendarUnit.MAIN_ALIASES;
    }

    private enum CalendarUnit {
        SECOND(TimeUnit.SECONDS.toMillis(1), "second", "seconds", "s", "sec", "secs"),
        MINUTE(TimeUnit.MINUTES.toMillis(1), "minute", "minutes", "m", "min", "mins"),
        HOUR(TimeUnit.HOURS.toMillis(1), "hour", "hrs", "h", "hr", "hours"),
        DAY(TimeUnit.DAYS.toMillis(1), "day", "days", "d"),
        WEEK(TimeUnit.DAYS.toMillis(1) * 7L, "week", "weeks", "w", "wk", "wks"),
        MONTH(TimeUnit.DAYS.toMillis(1) * 30L, "month", "months", "mn", "mns"), // 30 days is close enough...
        YEAR(TimeUnit.DAYS.toMillis(1) * 365L, "year", "years", "y", "yr", "yrs");

        private static ImmutableList<String> ALL_ALIASES = ImmutableList.copyOf(getAllAliases());
        private static ImmutableMap<String, CalendarUnit> ALIAS_MAP = getAliasMap();
        private static ImmutableList<String> MAIN_ALIASES = ImmutableList.copyOf(getMainAliases());

        private static Collection<String> getAllAliases( ) {
            ArrayList<String> aliases = new ArrayList<>();
            for (CalendarUnit c : values()) {
                Collections.addAll(aliases, c.aliases);
            }
            Collections.sort(aliases, STRING_LENGTH_COMPARATOR); // Sort them by largest to smallest so that the regex doesn't match a smaller value first.
            return aliases;
        }

        private static ImmutableMap<String, CalendarUnit> getAliasMap( ) {
            ImmutableMap.Builder<String, CalendarUnit> b = ImmutableMap.builder();
            for (CalendarUnit c : values()) {
                for (String a : c.aliases) {
                    b.put(a, c);
                }
            }
            return b.build();
        }

        private static Collection<String> getMainAliases( ) {
            ArrayList<String> aliases = new ArrayList<>();
            for (CalendarUnit c : values()) {
                aliases.add(c.mainAlias);
            }
            Collections.sort(aliases, STRING_LENGTH_COMPARATOR); // Sort them by largest to smallest so that the regex doesn't match a smaller value first.
            return aliases;
        }

        private final long timeInMillis;
        private final String[] aliases;
        private final String mainAlias;
        private final String mainAliasPlural;

        CalendarUnit(long timeInMillis, String mainAlias, String mainAliasPlural, String... aliases) {
            this.timeInMillis = timeInMillis;
            this.mainAlias = mainAlias;
            this.mainAliasPlural = mainAliasPlural;
            String[] newAliases = new String[aliases.length + 2];
            newAliases[0] = mainAlias;
            newAliases[1] = mainAliasPlural;
            System.arraycopy(aliases, 0, newAliases, 2, aliases.length);
            this.aliases = newAliases;
        }
    }
}
