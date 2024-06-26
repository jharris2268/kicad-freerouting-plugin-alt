/*
 *
 *   Copyright (C) 2014 Andrey Belomutskiy
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
 *
 */

package eu.mihosoft.freerouting.interactive;

import eu.mihosoft.freerouting.board.LayerStructure;
import eu.mihosoft.freerouting.board.RoutingBoard;
import eu.mihosoft.freerouting.board.TestLevel;
import eu.mihosoft.freerouting.geometry.planar.IntBox;
import eu.mihosoft.freerouting.geometry.planar.PolylineShape;
import eu.mihosoft.freerouting.rules.BoardRules;

/**
 * Andrey Belomutskiy
 * 6/28/2014
 */
public interface IBoardHandling {

    RoutingBoard get_routing_board();

    void create_board(IntBox p_bounding_box, LayerStructure p_layer_structure,
                      PolylineShape[] p_outline_shapes, String p_outline_clearance_class_name,
                      BoardRules p_rules, eu.mihosoft.freerouting.board.Communication p_board_communication, TestLevel p_test_level);

    Settings get_settings();
}
