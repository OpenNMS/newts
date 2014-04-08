/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.api;


import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class Duration implements Comparable<Duration> {

    private final long m_duration;
    private final TimeUnit m_unit;

    public Duration(long duration, TimeUnit unit) {
        m_duration = duration;
        m_unit = unit;
    }

    public long convert(TimeUnit unit) {
        return unit.convert(getDuration(), getUnit());
    }

    public long asMillis() {
        return convert(TimeUnit.MILLISECONDS);
    }

    public long asSeconds() {
        return convert(TimeUnit.SECONDS);
    }

    public static Duration seconds(long seconds) {
        return new Duration(seconds, TimeUnit.SECONDS);
    }

    public static Duration millis(long millis) {
        return new Duration(millis, TimeUnit.MILLISECONDS);
    }
    
    public static Duration minutes(long minutes) {
        return new Duration(minutes, TimeUnit.MINUTES);
    }

    public long getDuration() {
        return m_duration;
    }

    public TimeUnit getUnit() {
        return m_unit;
    }

    public Duration plus(Duration o) {
        TimeUnit u = Timestamp.finest(getUnit(), o.getUnit());
        return new Duration(convert(u) + o.convert(u), u);
    }

    public Duration times(long value) {
        return new Duration(getDuration() * value, getUnit());
    }

    public long divideBy(Duration o) {
        TimeUnit u = Timestamp.finest(getUnit(), o.getUnit());
        return convert(u) / o.convert(u);
    }

    public boolean isMultiple(Duration o) {
        TimeUnit u = Timestamp.finest(getUnit(), o.getUnit());
        return (this.gt(o) && ((convert(u) % o.convert(u)) == 0));
    }

    @Override
    public String toString() {
        return String.format("%s[%d, %s]", getClass().getSimpleName(), m_duration, getUnit());
    }

    @Override
    public int compareTo(Duration o) {
        TimeUnit unit = Timestamp.finest(getUnit(), o.getUnit());
        return convert(unit) < o.convert(unit) ? -1 : (convert(unit) > o.convert(unit) ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Duration)) return false;
        return compareTo((Duration) o) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Long.valueOf(convert(TimeUnit.NANOSECONDS)));
    }

    public boolean lt(Duration o) {
        return compareTo(o) == -1;
    }

    public boolean gt(Duration o) {
        return compareTo(o) == 1;
    }

    public boolean lte(Duration o) {
        return compareTo(o) <= 0;
    }

    public boolean gte(Duration o) {
        return compareTo(o) >= 0;
    }

}
