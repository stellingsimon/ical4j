/**
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.ical4j.model

import net.fortuna.ical4j.model.component.VEvent

import java.time.ZoneId

class VEventRecurrenceTest extends GroovyTestCase {

	void testCalculateRecurrenceSet() {
		VEvent event = new ContentBuilder().vevent {
			dtstart('20101113', parameters: parameters() {
				value('DATE')})
			dtend('20101114', parameters: parameters() {
				value('DATE')})
			rrule('FREQ=WEEKLY;WKST=MO;INTERVAL=3;BYDAY=MO,TU,SA')
		}
		
		def dates = event.calculateRecurrenceSet(new Period('20101101T000000/20110101T000000'))
		
		def expected = new PeriodList(true)
		expected.add new Period('20101113T000000Z/P1D')
		expected.add new Period('20101129T000000Z/P1D')
		expected.add new Period('20101130T000000Z/P1D')
		expected.add new Period('20101204T000000Z/P1D')
		expected.add new Period('20101220T000000Z/P1D')
		expected.add new Period('20101221T000000Z/P1D')
		expected.add new Period('20101225T000000Z/P1D')
		
		println dates
		assert dates == expected
	}

	void testForDailyEventsWhereExDateIsDate() {
		VEvent event = new ContentBuilder().vevent {
			dtstart('20101113', parameters: parameters() { value('DATE') })
			dtend('20101114', parameters: parameters() { value('DATE') })
			rrule('FREQ=WEEKLY;WKST=MO;INTERVAL=3;BYDAY=MO,TU,SA')
			exdate('20101129,20101204', parameters: parameters() { value('DATE') })
		}
		def expectedStr = ['20101113T000000Z/P1D', '20101130T000000Z/P1D', '20101220T000000Z/P1D', '20101221T000000Z/P1D', '20101225T000000Z/P1D'];
		def expected = new PeriodList(true)
		expectedStr.each { expected.add(new Period(it)) }

		def actual = event.calculateRecurrenceSet(new Period('20101101T000000/20110101T000000'))

		println actual
		assert actual == expected
	}

	/**
	 * This is a regression test for issue #117.
	 */
	void testRecurringInstanceInDSTGap() {
		// This event starts on the Sat before the switch from standard time to daylight saving time:
		VEvent event = new ContentBuilder().vevent {
			dtstart('20180324T023000')
			dtend('20180324T024500')
			rrule('FREQ=DAILY;INTERVAL=1;COUNT=4')
		}

		def actual = event.calculateRecurrenceSet(new Period('20180301T000000/20180331T000000'))

		def expectedStr = ['20180324T023000/PT15M', '20180325T033000/PT15M', '20180326T023000/PT15M', '20180327T023000/PT15M']
		def expected = new PeriodList(false)
		expectedStr.each { expected.add(new Period(it)) }

		def expectedInstants = expected.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }
		def actualInstants = actual.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }

		println "expected: " + expectedInstants
		println "  actual: " + actualInstants
		assert actualInstants == expectedInstants
	}

	/**
	 * This is a regression test for issue #117.
	 */
	void testRecurringInstanceInDSTDoubledHour() {
		// This event starts on the Sat before the switch from daylight saving time to standard time:
		VEvent event = new ContentBuilder().vevent {
			dtstart('20181026T023000')
			dtend('20181026T024500')
			rrule('FREQ=DAILY;INTERVAL=1;COUNT=4')
		}

		def actual = event.calculateRecurrenceSet(new Period('20181001T000000/20181031T000000'))

		def expectedStr = ['20181026T023000/PT15M', '20181027T023000/PT15M', '20181028T023000/PT15M', '20181029T023000/PT15M']
		def expected = new PeriodList(false)
		expectedStr.each { expected.add(new Period(it)) }

		def expectedInstants = expected.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }
		def actualInstants = actual.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }

		println "expected: " + expectedInstants
		println "  actual: " + actualInstants
		assert actualInstants == expectedInstants
	}

	/**
	 * This is a regression test for issue #117.
	 */
	void testRecurringInstanceInDSTGapSkipped() {
		// This event starts on the Sat before the switch from standard time to daylight saving time:
		VEvent event = new ContentBuilder().vevent {
			dtstart('20180324T023000')
			dtend('20180324T024500')
			rrule('FREQ=DAILY;INTERVAL=2;COUNT=2')
		}

		def actual = event.calculateRecurrenceSet(new Period('20180301T000000/20180331T000000'))

		def expectedStr = ['20180324T023000/PT15M', '20180326T023000/PT15M']
		def expected = new PeriodList(false)
		expectedStr.each { expected.add(new Period(it)) }

		def expectedInstants = expected.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }
		def actualInstants = actual.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }

		println "expected: " + expectedInstants
		println "  actual: " + actualInstants
		assert actualInstants == expectedInstants
	}

	/**
	 * This is a regression test for issue #117.
	 */
	void testRecurringInstanceInDSTDoubledHourSkipped() {
		// This event starts on the Sat before the switch from daylight saving time to standard time:
		VEvent event = new ContentBuilder().vevent {
			dtstart('20181026T023000')
			dtend('20181026T024500')
			rrule('FREQ=DAILY;INTERVAL=2;COUNT=2')
		}

		def actual = event.calculateRecurrenceSet(new Period('20181001T000000/20181031T000000'))

		def expectedStr = ['20181026T023000/PT15M', '20181028T023000/PT15M']
		def expected = new PeriodList(false)
		expectedStr.each { expected.add(new Period(it)) }

		def expectedInstants = expected.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }
		def actualInstants = actual.collect { it.start.toInstant().atZone(ZoneId.systemDefault()).toString() }

		println "expected: " + expectedInstants
		println "  actual: " + actualInstants
		assert actualInstants == expectedInstants
	}
}
