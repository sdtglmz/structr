/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.Date;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.schema.parser.DatePropertyParser;

/**
* A property that stores and retrieves a simple string-based Date with
* the given date format pattern. This property uses a long value internally
* to provide millisecond precision.
 *
 *
 */
public class DateProperty extends AbstractPrimitiveProperty<Date> {

	public DateProperty(final String name) {
		super(name);
		this.format = getDefaultFormat();
	}

	public DateProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
		this.format = getDefaultFormat();
	}

	public DateProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);

		if (StringUtils.isNotBlank(format)) {
			this.format = format;
		} else {
			this.format = getDefaultFormat();
		}
	}

	@Override
	public String typeName() {
		return "Date";
	}

	@Override
	public Class valueType() {
		return Date.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Long;
	}

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<String, Date> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Long) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number)value).longValue();
			}

			try {

				return Long.parseLong(value.toString());

			} catch (Throwable t) {
			}

			try {

				return DatePropertyParser.parse(value.toString(), format).getTime();

			} catch (Throwable t) {
			}
		}

		return null;
	}

	private class DatabaseConverter extends PropertyConverter<Date, Long> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public Long convert(Date source) throws FrameworkException {

			if (source != null) {

				return source.getTime();
			}

			return null;
		}

		@Override
		public Date revert(Long source) throws FrameworkException {

			if (source != null) {

				return new Date(source);
			}

			return null;

		}
	}

	private class InputConverter extends PropertyConverter<String, Date> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Date convert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				Date result = DatePropertyParser.parse(source, format);

				if (result != null) {
					return result;
				}

				throw new FrameworkException(422, "Cannot parse input for property " + jsonName(), new DateFormatToken(declaringClass.getSimpleName(), DateProperty.this));

			}

			return null;

		}

		@Override
		public String revert(Date source) throws FrameworkException {

			return DatePropertyParser.format(source, format);
		}

	}

	// ----- CMIS support -----
	@Override
	public PropertyType getDataType() {
		return PropertyType.DATETIME;
	}

	// ----- static methods -----
	public static String getDefaultFormat() {
		return Settings.DefaultDateFormat.getValue();
	}
}
