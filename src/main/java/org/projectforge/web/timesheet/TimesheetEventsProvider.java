/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.timesheet;

import java.util.List;

import net.ftlines.wicket.fullcalendar.Event;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.projectforge.calendar.TimePeriod;
import org.projectforge.common.DateHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.core.OrderDirection;
import org.projectforge.fibu.ProjektDO;
import org.projectforge.fibu.kost.Kost2DO;
import org.projectforge.task.TaskDO;
import org.projectforge.timesheet.TimesheetDO;
import org.projectforge.timesheet.TimesheetDao;
import org.projectforge.timesheet.TimesheetFilter;
import org.projectforge.user.PFUserContext;
import org.projectforge.web.HtmlHelper;
import org.projectforge.web.calendar.CalendarFilter;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

/**
 * Creates events for FullCalendar.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimesheetEventsProvider extends MyFullCalendarEventsProvider
{
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetEventsProvider.class);

	private static final long serialVersionUID = 2241430630558260146L;

	private final TimesheetDao timesheetDao;

	private final CalendarFilter calFilter;

	private long totalDuration;

	private Integer month;

	private DateTime firstDayOfMonth;

	private int days;

	// duration by day of month.
	private final long[] durationsPerDayOfMonth = new long[32];

	private final long[] durationsPerDayOfYear = new long[380];

	/**
	 * @param parent For i18n.
	 * @param timesheetDao
	 * @param calFilter
	 * @see Component#getString(String)
	 */
	public TimesheetEventsProvider(final Component parent, final TimesheetDao timesheetDao, final CalendarFilter calFilter)
	{
		super(parent);
		this.timesheetDao = timesheetDao;
		this.calFilter = calFilter;
	}

	/**
	 * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
	 */
	@Override
	protected void buildEvents(final DateTime start, final DateTime end)
	{
		totalDuration = 0;
		for (int i = 0; i < durationsPerDayOfMonth.length; i++) {
			durationsPerDayOfMonth[i] = 0;
		}
		for (int i = 0; i < durationsPerDayOfYear.length; i++) {
			durationsPerDayOfYear[i] = 0;
		}
		final Integer userId = calFilter.getUserId();
		if (userId == null) {
			return;
		}
		final TimesheetFilter filter = new TimesheetFilter();
		filter.setUserId(userId);
		filter.setStartTime(start.toDate());
		filter.setStopTime(end.toDate());
		filter.setOrderType(OrderDirection.ASC);
		final List<TimesheetDO> timesheets = timesheetDao.getList(filter);
		boolean longFormat = false;
		days = Days.daysBetween(start, end).getDays();
		if (days < 10) {
			// Week or day view:
				longFormat = true;
				month = null;
				firstDayOfMonth = null;
		} else {
			// Month view:
			final DateTime currentMonth = new DateTime(start.plusDays(10), PFUserContext.getDateTimeZone()); // Now we're definitely in the right
			// month.
			month = currentMonth.getMonthOfYear();
			firstDayOfMonth = currentMonth.withDayOfMonth(1);
		}
		if (CollectionUtils.isNotEmpty(timesheets) == true) {
			for (final TimesheetDO timesheet : timesheets) {
				final DateTime startTime = new DateTime(timesheet.getStartTime(), PFUserContext.getDateTimeZone());
				final DateTime stopTime = new DateTime(timesheet.getStopTime(), PFUserContext.getDateTimeZone());
				if (stopTime.isBefore(start) == true || startTime.isAfter(end) == true) {
					// Time sheet doesn't match time period start - end.
					continue;
				}
				final long duration = timesheet.getDuration();
				final Event event = new Event();
				final String id = "ts-" + timesheet.getId();
				event.setId("" + id);
				event.setStart(startTime);
				event.setEnd(stopTime);
				final String title = getTitle(timesheet);
				if (longFormat == true) {
					// Week or day view:
					event.setTitle(title + "\n" + getToolTip(timesheet) + "\n" + formatDuration(duration, false));
				} else {
					// Month view:
					event.setTitle(title);
				}
				if (month != null && startTime.getMonthOfYear() != month && stopTime.getMonthOfYear() != month) {
					// Display time sheets of other month as grey blue:
					event.setTextColor("#222222").setBackgroundColor("#ACD9E8").setColor("#ACD9E8");
				}
				events.put(id, event);
				if (month == null || startTime.getMonthOfYear() == month) {
					totalDuration += duration;
					addDurationOfDay(startTime.getDayOfMonth(), duration);
				}
				final int dayOfYear = startTime.getDayOfYear();
				addDurationOfDayOfYear(dayOfYear, duration);
			}
		}
		if (calFilter.isShowStatistics() == true) {
			// Show statistics: duration of every day is shown as all day event.
			DateTime day = start;
			int paranoiaCounter = 0;
			do {
				if (++paranoiaCounter > 1000) {
					log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
					break;
				}
				final int dayOfMonth = day.getDayOfMonth();
				final int dayOfYear = day.getDayOfYear();
				final long duration = durationsPerDayOfMonth[dayOfMonth];
				final boolean firstDayOfWeek = day.getDayOfWeek() == PFUserContext.getJodaFirstDayOfWeek();
				if (firstDayOfWeek == false && duration == 0) {
					day = day.plusDays(1);
					continue;
				}
				final Event event = new Event().setAllDay(true);
				final String id = "s-" + (dayOfYear);
				event.setId(id);
				event.setStart(day);
				final String durationString = formatDuration(duration, false);
				if (firstDayOfWeek == true) {
					// Show week of year at top of first day of week.
					long weekDuration = 0;
					for (short i = 0; i < 7; i++) {
						weekDuration += durationsPerDayOfYear[dayOfYear + i];
					}
					final StringBuffer buf = new StringBuffer();
					buf.append(getString("calendar.weekOfYearShortLabel")).append(DateHelper.getWeekOfYear(day));
					if (days > 1 && weekDuration > 0) {
						// Show total sum of durations over all time sheets of current week (only in week and month view).
						buf.append(": ").append(formatDuration(weekDuration, false));
					}
					if (duration > 0) {
						buf.append(", ").append(durationString);
					}
					event.setTitle(buf.toString());
				} else {
					event.setTitle(durationString);
				}
				event.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9");
				event.setEditable(false);
				events.put(id, event);
				day = day.plusDays(1);
			} while (day.isAfter(end) == false);
		}
	}

	public String formatDuration(final long millis)
	{
		return formatDuration(millis, firstDayOfMonth != null);
	}

	private String formatDuration(final long millis, final boolean showTimePeriod)
	{
		final int[] fields = TimePeriod.getDurationFields(millis, 8, 200);
		final StringBuffer buf = new StringBuffer();
		if (fields[0] > 0) {
			buf.append(fields[0]).append(getString("calendar.unit.day")).append(" ");
		}
		buf.append(fields[1]).append(":").append(StringHelper.format2DigitNumber(fields[2])).append(getString("calendar.unit.hour"));
		if (showTimePeriod == true) {
			buf.append(" (").append(getString("calendar.month")).append(")");
		}
		return buf.toString();
	}

	private String getTitle(final TimesheetDO timesheet)
	{
		final Kost2DO kost2 = timesheet.getKost2();
		final TaskDO task = timesheet.getTask();
		if (kost2 == null) {
			return (task != null && task.getTitle() != null) ? HtmlHelper.escapeXml(task.getTitle()) : "";
		}
		final StringBuffer buf = new StringBuffer();
		final StringBuffer b2 = new StringBuffer();
		final ProjektDO projekt = kost2.getProjekt();
		if (projekt != null) {
			// final KundeDO kunde = projekt.getKunde();
			// if (kunde != null) {
			// if (StringUtils.isNotBlank(kunde.getIdentifier()) == true) {
			// b2.append(kunde.getIdentifier());
			// } else {
			// b2.append(kunde.getName());
			// }
			// b2.append(" - ");
			// }
			if (StringUtils.isNotBlank(projekt.getIdentifier()) == true) {
				b2.append(projekt.getIdentifier());
			} else {
				b2.append(projekt.getName());
			}
		} else {
			b2.append(kost2.getDescription());
		}
		buf.append(StringUtils.abbreviate(b2.toString(), 30));
		return buf.toString();
	}

	private String getToolTip(final TimesheetDO timesheet)
	{
		final String location = timesheet.getLocation();
		final String description = timesheet.getShortDescription();
		final TaskDO task = timesheet.getTask();
		final StringBuffer buf = new StringBuffer();
		if (StringUtils.isNotBlank(location) == true) {
			buf.append(location);
			if (StringUtils.isNotBlank(description) == true) {
				buf.append(": ");
			}
		}
		buf.append(description);
		if (timesheet.getKost2() == null) {
			buf.append("; \n").append(task.getTitle());
		}
		return buf.toString();
	}

	/**
	 * @return the duration
	 */
	public long getTotalDuration()
	{
		return totalDuration;
	}

	private void addDurationOfDay(final int dayOfMonth, final long duration)
	{
		durationsPerDayOfMonth[dayOfMonth] += duration;
	}

	/**
	 * @param dayOfMonth
	 * @see DateTime#getDayOfMonth()
	 */
	public long getDurationOfDay(final int dayOfMonth)
	{
		return durationsPerDayOfMonth[dayOfMonth];
	}

	private void addDurationOfDayOfYear(final int dayOfYear, final long duration)
	{
		durationsPerDayOfYear[dayOfYear] += duration;
	}

	/**
	 * @param weekOfYear
	 * @see DateTime#getDayOfMonth()
	 */
	public long getDurationOfWeekOfYear(final int weekOfYear)
	{
		return durationsPerDayOfMonth[weekOfYear];
	}
}