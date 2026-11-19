# Expected results

## pom.xml (Maven, licences from deps.dev)

Test-scope dependencies are skipped. Multiple deps.dev licences are joined with AND.

| Dependency | Version | deps.dev `licenses` | Expression | Verdict | Note |
|---|---|---|---|---|---|
| com.google.guava:guava | 33.0.0-jre | `Apache-2.0` | Apache-2.0 | APPROVED | |
| org.postgresql:postgresql | 42.7.2 | `BSD-2-Clause` | BSD-2-Clause | APPROVED | |
| org.hibernate:hibernate-core | `${hibernate.version}` = 5.6.15.Final | `LGPL-2.1-or-later` | LGPL-2.1-or-later | REVIEW | Version resolved from property |
| org.mariadb.jdbc:mariadb-java-client | 3.3.3 | `LGPL-2.1` | LGPL-2.1 | REVIEW | Deprecated id, alias to LGPL-2.1-only |
| com.itextpdf:kernel | `${itext.version}` = 8.0.4 | `AGPL-3.0` | AGPL-3.0 | DENIED | Deprecated id, alias to AGPL-3.0-only |
| com.mysql:mysql-connector-j | 8.3.0 | `non-standard` | non-standard | UNKNOWN | Not a listed id |
| jakarta.ws.rs:jakarta.ws.rs-api | 3.1.0 | `EPL-2.0`, `GPL-2.0-with-classpath-exception` | EPL-2.0 AND GPL-2.0-with-classpath-exception | UNKNOWN | AND takes the more severe side; the second id is the SPDX-deprecated spelling, not in the policy list and not a `WITH` expression, so UNKNOWN (EPL-2.0 alone is REVIEW) |
| mysql:mysql-connector-java | 8.0.33 | `[]` (empty) | (none) | UNKNOWN | No licence at all |
| org.junit.jupiter:junit-jupiter | 5.10.2 | (`non-standard`) | | skipped | `<scope>test</scope>` |

Totals (8 checked, 1 skipped): APPROVED 2, REVIEW 2, UNKNOWN 3, DENIED 1.

Caveat: no real artifact found whose deps.dev licence is a literal `X WITH Y` string. Jakarta/glassfish artifacts report `GPL-2.0-with-classpath-exception` as a separate entry instead. To unit-test the `WITH` rules, use synthetic input.

## package-lock.json (npm, lockfile v3, devDependency left-pad ignored)

18 non-dev packages (excluding the root entry).

| Package | Version | `license` | Verdict | Note |
|---|---|---|---|---|
| @ckeditor/ckeditor5-utils | 40.2.0 | `GPL-2.0-or-later` | DENIED | |
| axe-core | 4.9.1 | `MPL-2.0` | REVIEW | |
| highcharts | 11.4.8 | `https://www.highcharts.com/license` | UNKNOWN | Not an SPDX id |
| pako | 1.0.11 | `(MIT AND Zlib)` | UNKNOWN | Zlib is not listed, AND takes the more severe side |
| jszip | 3.10.1 | `(MIT OR GPL-3.0-or-later)` | APPROVED | OR takes the less severe side |
| lodash, lodash-es | 4.17.21 | `MIT` | APPROVED | |
| core-util-is, immediate, isarray, lie, process-nextick-args, readable-stream, safe-buffer, setimmediate, string_decoder, util-deprecate | various | `MIT` | APPROVED | |
| inherits | 2.0.4 | `ISC` | APPROVED | |
| left-pad (dev) | 1.3.0 | `WTFPL` | ignored | `"dev": true` (would be UNKNOWN) |

Totals: APPROVED 14, REVIEW 1, UNKNOWN 2, DENIED 1 (sum 18).
