/*
 *
 *   Copyright (C) 2014 Andrey Belomutskiy
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
 *
 */

package eu.mihosoft.freerouting.interactive;

import eu.mihosoft.freerouting.board.Communication;
import eu.mihosoft.freerouting.board.LayerStructure;
import eu.mihosoft.freerouting.board.RoutingBoard;
import eu.mihosoft.freerouting.board.TestLevel;
import eu.mihosoft.freerouting.geometry.planar.IntBox;
import eu.mihosoft.freerouting.geometry.planar.PolylineShape;
import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.rules.BoardRules;

/**
 * Base implementation for headless mode
 *
 * Andrey Belomutskiy
 * 6/28/2014
 */
public class BoardHandlingImpl implements IBoardHandling {
    /** The current settings for interactive actions on the board */
    public Settings settings = null;
    /** The board database used in this interactive handling. */
    protected RoutingBoard board = null;

    public BoardHandlingImpl() {
    }

    /**
     * Gets the routing board of this board handling.
     */
    @Override
    public RoutingBoard get_routing_board()
    {
        return this.board;
    }

    @Override
    public Settings get_settings() {
        return settings;
    }

    @Override
    public void create_board(IntBox p_bounding_box, LayerStructure p_layer_structure, PolylineShape[] p_outline_shapes, String p_outline_clearance_class_name, BoardRules p_rules, Communication p_board_communication, TestLevel p_test_level) {
        if (this.board != null)
        {
            FRLogger.warn(" BoardHandling.create_board: eu.mihosoft.freerouting.board already created");
        }
        int outline_cl_class_no = 0;

        if (p_rules != null)
        {
            if (p_outline_clearance_class_name != null && p_rules.clearance_matrix != null)
            {
                outline_cl_class_no = p_rules.clearance_matrix.get_no(p_outline_clearance_class_name);
                outline_cl_class_no = Math.max(outline_cl_class_no, 0);
            }
            else
            {
                outline_cl_class_no =
                        p_rules.get_default_net_class().default_item_clearance_classes.get(eu.mihosoft.freerouting.rules.DefaultItemClearanceClasses.ItemClass.AREA);
            }
        }
        this.board =
                new RoutingBoard(p_bounding_box, p_layer_structure, p_outline_shapes, outline_cl_class_no,
                        p_rules, p_board_communication, p_test_level);

        this.settings = new Settings(this.board);//, this.activityReplayFile);
    }

}
