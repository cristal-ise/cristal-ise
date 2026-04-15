/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
Query("QueryBasicItemList", 0) {
    parameter(name: 'domainPath', type: 'java.lang.String')
    parameter(name: 'searchText', type: 'java.lang.String')
    parameter(name: 'offset', type: 'java.lang.Integer')
    parameter(name: 'limit', type: 'java.lang.Integer')
    rootElement('BasicItemList')
    recordElement('Item')
    query(language: "sql") {
'''
WITH
    params(fullTextSearch) AS (VALUES ('@{searchText}')),
    pivoted AS (
        SELECT ip."UUID",
               MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Module')  AS "Module",
               MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Type')    AS "Type",
               MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Name')    AS "Name",
               MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Version') AS "Version"
        FROM "ITEM_PROPERTY" ip
                 LEFT JOIN "DOMAIN_PATH" dp ON ip."UUID" = dp."TARGET"
        WHERE dp."PATH" LIKE '@{domainPath}%'
        GROUP BY ip."UUID", dp."PATH"
    )
SELECT p."UUID", p."Module", p."Type", p."Name", p."Version", COUNT(*) OVER() AS "TotalCount"
FROM pivoted p
    CROSS JOIN params
WHERE fullTextSearch = ''
   OR COALESCE("Module", '')  ILIKE '%' || fullTextSearch || '%'
   OR COALESCE("Type", '')    ILIKE '%' || fullTextSearch || '%'
   OR COALESCE("Name", '')    ILIKE '%' || fullTextSearch || '%'
   OR COALESCE("Version", '') ILIKE '%' || fullTextSearch || '%'
ORDER BY "Type", "Name", "Version"
OFFSET @{offset} ROWS FETCH NEXT @{limit} ROWS ONLY;
'''
    }
}
