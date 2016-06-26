# F-ANSI - Java Colored Output

[![Build Status](https://drone.io/bitbucket.org/dimo414/f-ansi/status.png)](https://drone.io/bitbucket.org/dimo414/f-ansi/latest)

A fluent Java API to control console output via ANSI control codes

## Background

Console output doesn't have to be boring black and white. But creating visually
appealing command line programs isn't straightforward. The standard mechanism,
[ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code), are
confusing and tedious to work with. F-ANSI provides an intuitive, fluent
abstraction to create elegant applications without the hassle.

## Usage

**In Alpha**: F-ANSI is still in active development, and the API may change.

API Javadocs: http://dimo414.bitbucket.org/f-ansi/

F-ANSI's fluent interface allows you to compose readable chains of output; the
following example prints "Foo" in red, followed by a hyphen in the terminal's
default styling, folowed by "Bar" in green.

    ansi().color(RED).out("Foo").out(" - ").color(LIGHT_GREEN).outln("Bar");

![Colored 'Foo - Bar'](/images/FooBar.png)

There are several demo scripts in the [demo](/demo/demo) directory you can use
for reference and testing. To print a color table of the default ANSI colors,
you can call `demo.ColorTable`:

![ANSI Color Table](/images/ColorTable.png)

## Dependencies

F-ANSI depends on [Guava](https://github.com/google/guava). The tests further
depend on [TestNG](testng.org/) and [Truth](https://github.com/google/truth).

For conveinence the `f-ansi-no-dependencies.jar` bundles in Guava, at the cost
of a much larger Jar. If you already have Guava on your classpath, the
`f-ansi.jar` is much smaller.

## Copyright

Copyright 2016 Michael Diamond

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