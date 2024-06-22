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
 * BoardPanel.java
 *
 * Created on 3. Oktober 2002, 18:47
 */

package eu.mihosoft.freerouting.gui;

import eu.mihosoft.freerouting.interactive.BoardHandling;
import eu.mihosoft.freerouting.logger.FRLogger;


/**
 *
 * Panel containing the graphical representation of a routing board.
 *
 * @author Alfons Wirtz
 */
public class BoardPanel
{
    
    /** Creates a new BoardPanel in an Application */
    public BoardPanel(BoardFrame p_board_frame)
    {
        board_frame = p_board_frame;
        default_init();
    }
    
    private void default_init()
    {
        board_handling = new BoardHandling(this);
    }
    
    public final BoardFrame board_frame;
    
    BoardHandling board_handling = null;
    
}
