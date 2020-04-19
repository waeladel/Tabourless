
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
}