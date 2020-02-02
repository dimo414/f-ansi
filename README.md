# F-ANSI - Java Colored Output

[![0.3 Release](https://img.shields.io/badge/release-0.3-green.svg)](https://github.com/dimo414/f-ansi/releases/0.3)
[![0.3 API docs](https://img.shields.io/badge/API-0.3-red.svg)](https://dimo414.bitbucket.io/f-ansi/)
[![Open Issues](https://img.shields.io/github/issues/dimo414/f-ansi.svg)](https://github.com/dimo414/f-ansi/issues)
[![GPL License](https://img.shields.io/badge/license-GPL-blue.svg)](https://github.com/dimo414/f-ansi/blob/master/LICENSE)

*A fluent Java API to control console output via ANSI control codes*

## Background

Console output doesn't have to be boring black and white, but creating visually
appealing command line programs isn't straightforward. The standard mechanism,
[ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code), are
confusing and tedious to work with. F-ANSI provides an intuitive, fluent
abstraction to create elegant applications without the hassle.

## Usage

**In Beta**: F-ANSI is still in active development, and the API may change. See
Guava's
[Beta API](https://github.com/google/guava/wiki/PhilosophyExplained#beta-apis)
policy for more.

F-ANSI's fluent interface allows you to compose readable chains of output; the
following example prints "Foo" in red, followed by a hyphen in the terminal's
default styling, followed by "Bar" in green.

    ansi().color(RED).out("Foo").out(" - ").color(LIGHT_GREEN).outln("Bar");

![Colored 'Foo - Bar'](/images/FooBar.png)

There are several demo scripts in the [demo](/demo/demo) directory you can use
for reference and testing. To print a color table of the default ANSI colors,
you can call `demo.ColorTable`:

![ANSI Color Table](/images/ColorTable.png)

## Dependencies

F-ANSI depends on [Guava](https://github.com/google/guava). The tests further
depend on [TestNG](testng.org/) and [Truth](https://github.com/google/truth).

For convenience the `f-ansi-no-dependencies.jar` bundles in Guava, at the cost
of a much larger Jar. If you already have Guava on your classpath, the
`f-ansi.jar` is much smaller.

## Copyright

Copyright 2016-2017 Michael Diamond

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
