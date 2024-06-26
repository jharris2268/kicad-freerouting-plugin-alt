/*
 *   Copyright (C) 2014  Alfons Wirtz
 *   website www.freerouting.net
 *
 *   Copyright (C) 2017 Michael Hoffer <info@michaelhoffer.de>
 *   Website www.freerouting.mihosoft.eu
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License at <http://www.gnu.org/licenses/> 
 *   for more details.
 *
 * TestLevel.java
 *
 * Created on 20. April 2006, 07:32
 *
 */

package eu.mihosoft.freerouting.board;

/**
 * If {@literal >} RELEASE, some features may be used, which are still in experimental state.
 * Also warnings for debugging may be printed depending on the test_level.
 *
 * @author Alfons Wirtz
 */
public enum TestLevel
{
    RELEASE_VERSION, TEST_VERSION
}
