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


import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class Timestamp implements Comparable<Timestamp>, Serializable {
    private static final long serialVersionUID = 8827895112055791967L;

    public final long m_time;
    public final TimeUnit m_unit;

    public Timestamp(long time, TimeUnit unit) {
        m_time = time;
        m_unit = unit;
    }

    private long convert(TimeUnit unit) {
        return unit.convert(m_time, m_unit);
    }

    public long asSeconds() {
        return convert(TimeUnit.SECONDS);
    }

    public long asMillis() {
        return convert(TimeUnit.MILLISECONDS);
    }

    public Date asDate() {
        return new Date(convert(TimeUnit.MILLISECONDS));
    }

    public TimeUnit getUnit() {
        return m_unit;
    }

    public Timestamp plus(long value, TimeUnit units) {
        TimeUnit finest = finest(m_unit, units);
        return new Timestamp(convert(finest) + finest.convert(value, units), finest);
    }

    public Timestamp plus(Duration d) {
        TimeUnit finest = finest(m_unit, d.getUnit());
        return new Timestamp(convert(finest) + d.convert(finest), finest);
    }

    public Timestamp minus(long value, TimeUnit units) {
        TimeUnit finest = finest(m_unit, units);
        return new Timestamp(convert(finest) - finest.convert(value, units), finest);
    }

    public Timestamp minus(Duration d) {
        TimeUnit finest = finest(m_unit, d.getUnit());
        return new Timestamp(convert(finest) - d.convert(finest), finest);
    }

    public Duration minus(Timestamp t) {
        if (t.gt(this)) throw new IllegalArgumentException("you can only subtract an earlier date from a later one... negative durations don't make sense");
        TimeUnit finest = finest(m_unit, t.getUnit());
        return new Duration(convert(finest) - t.convert(finest), finest);

    }

    public boolean lt(Timestamp other) {
        return compareTo(other) < 0;
    }

    public boolean lte(Timestamp other) {
        return lt(other) || equals(other);
    }

    public boolean gt(Timestamp other) {
        return compareTo(other) > 0;
    }

    public boolean gte(Timestamp other) {
        return gt(other) || equals(other);
    }

    public Timestamp stepFloor(long stepSize, TimeUnit units) {
        return new Timestamp((convert(units) / stepSize) * stepSize, units);
    }

    public Timestamp stepFloor(Duration d) {
        return stepFloor(d.getDuration(), d.getUnit());
    }

    public Timestamp stepCeiling(long stepSize, TimeUnit units) {
        long v = convert(units);
        return ((v % stepSize) == 0) ? new Timestamp(v, units) : new Timestamp(((v / stepSize) + 1) * stepSize, units);
    }

    public Timestamp stepCeiling(Duration d) {
        return stepCeiling(d.getDuration(), d.getUnit());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Timestamp)) return false;
        return compareTo((Timestamp) other) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Long.valueOf(convert(TimeUnit.NANOSECONDS)));
    }

    @Override
    public int compareTo(Timestamp o) {
        TimeUnit unit = finest(getUnit(), o.getUnit());
        return convert(unit) < o.convert(unit) ? -1 : (convert(unit) > o.convert(unit) ? 1 : 0);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), asDate());
    }

    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static Timestamp fromEpochMillis(long millis) {
        return new Timestamp(millis, TimeUnit.MILLISECONDS);
    }

    public static Timestamp fromEpochSeconds(long seconds) {
        return new Timestamp(seconds, TimeUnit.SECONDS);
    }

    public static Timestamp fromDate(Date d) {
        return fromEpochMillis(d.getTime());
    }

    static boolean isFiner(TimeUnit unit1, TimeUnit unit2) {
        long c = unit2.convert(1, unit1);
        return c == 0;
    }

    static TimeUnit finest(TimeUnit unit1, TimeUnit unit2) {
        return isFiner(unit1, unit2) ? unit1 : unit2;
    }

}
