package com.overmc.overpermissions.internal.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.overmc.overpermissions.exceptions.TimeFormatException;

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
                BigDecimal msValue = value.multiply(BigDecimal.valueOf(unit.getTimeInMillis(1L)));
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

    public static ImmutableList<String> getTimeUnits( ) {
        return CalendarUnit.MAIN_ALIASES;
    }

    private static enum CalendarUnit {
        SECOND("s", "sec", "second", "secs", "seconds") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.SECONDS.toMillis(input);
            }
        },
        MINUTE("m", "min", "minute", "mins", "minutes") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.MINUTES.toMillis(input);
            }
        },
        HOUR("h", "hr", "hour", "hrs", "hours") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.HOURS.toMillis(input);
            }
        },
        DAY("d", "day", "days") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.DAYS.toMillis(input);
            }
        },
        WEEK("w", "wk", "week", "wks", "weeks") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.DAYS.toMillis(input) * 7L;
            }
        },
        MONTH("mn", "month", "mns", "months") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.DAYS.toMillis(input) * 30L; // 30 days is close enough...
            }
        },
        YEAR("y", "yr", "year", "yrs", "years") {
            @Override
            public long getTimeInMillis(long input) {
                return TimeUnit.DAYS.toMillis(input) * 365L;
            }
        };

        private static ImmutableList<String> ALL_ALIASES = ImmutableList.copyOf(getAllAliases());
        private static ImmutableMap<String, CalendarUnit> ALIAS_MAP = getAliasMap();
        private static ImmutableList<String> MAIN_ALIASES = ImmutableList.copyOf(getMainAliases());

        private static Collection<String> getAllAliases( ) {
            ArrayList<String> aliases = new ArrayList<>();
            for (CalendarUnit c : values()) {
                for (String a : c.aliases) {
                    aliases.add(a);
                }
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

        private final String[] aliases;
        private final String mainAlias;

        CalendarUnit(String... aliases) {
            this.aliases = aliases;
            this.mainAlias = aliases[0];
        }

        public abstract long getTimeInMillis(long input);
    }
}
