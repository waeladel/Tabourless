
/*
 * Copyright 2008-2010 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tabourless.queue.Utils;
import android.content.Context;

import com.tabourless.queue.R;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * Helper for date handle
 *
 * @author Jerome RADUGET
 */
public abstract class DateHelper {

    public static int getAge(final Date birthdate) {
        return getAge(Calendar.getInstance().getTime(), birthdate);
    }

    public static int getAge(final Date current, final Date birthdate) {

        if (birthdate == null) {
            return 0;
        }
        if (current == null) {
            return getAge(birthdate);
        } else {
            final Calendar c = new GregorianCalendar();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            c.setTimeInMillis(current.getTime() - birthdate.getTime());

            int result = 0;
            result = c.get(Calendar.YEAR) - 1970;
            result += (float) c.get(Calendar.MONTH) / (float) 12;
            return result;
        }

    }

    public static CharSequence getRelativeTime (long timePeriod, Context context) {

        Long second = TimeUnit.MILLISECONDS.toSeconds(timePeriod);
        Long minute = TimeUnit.MILLISECONDS.toMinutes(timePeriod);
        Long hour   = TimeUnit.MILLISECONDS.toHours(timePeriod);
        Long day  = TimeUnit.MILLISECONDS.toDays(timePeriod);

        String relativeTime = null;


        if (second < 60) {
            // seconds
            //relativeTime = second + " Seconds ";
            relativeTime = context.getResources().getQuantityString(R.plurals.seconds, second.intValue(), second.intValue());
        } else if (minute < 60) {
            // minutes
            //relativeTime = minute + " Minutes ";
            relativeTime = context.getResources().getQuantityString(R.plurals.minutes, minute.intValue(), minute.intValue());
        } else if (hour < 24) {
            // hours
            //relativeTime = hour + " Hours ";
            relativeTime = context.getResources().getQuantityString(R.plurals.hours, hour.intValue(), hour.intValue());
        } else if (day >= 7) {
            if (day > 360) {
                // Years
                //relativeTime = (day / 360) + " Years " ;
                Long days = day / 360;
                relativeTime = context.getResources().getQuantityString(R.plurals.years, days.intValue(), days.intValue());
            } else if (day > 30) {
                // Months
                //relativeTime = (day / 30) + " Months ";
                Long days = day / 30;
                relativeTime = context.getResources().getQuantityString(R.plurals.months, days.intValue(), days.intValue());
            } else {
                // Week
                //relativeTime = (day / 7) + " Week ";
                Long days = day / 7;
                relativeTime = context.getResources().getQuantityString(R.plurals.weeks, days.intValue(), days.intValue());
            }
        } else if (day < 7) {
            // day
            //relativeTime = day+" Days ";
            relativeTime = context.getResources().getQuantityString(R.plurals.days, day.intValue(), day.intValue());
        }

        //CharSequence ago = DateUtils.getRelativeTimeSpanString((now - timePeriod) , now, DateUtils.MINUTE_IN_MILLIS);
        return relativeTime;

    }

}