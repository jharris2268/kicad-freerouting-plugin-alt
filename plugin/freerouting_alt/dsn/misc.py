from . import s_tuple_parser as stp
import pcbnew

def get_board_layers(board):
    return [(i,board.GetLayerName(i),pcbnew.LAYER_ShowType(board.GetLayerType(i))) for i in board.GetDesignSettings().GetEnabledLayers().Seq()]

    


TU = lambda vals: stp.Tuple(vals)
NL = lambda sp=0: stp.Whitespace("\n"+(" "*sp))
SP = lambda: stp.Whitespace(" ")
LA = lambda la: stp.Label(str(la))
QS = lambda la: stp.QuotedString(str(la))


reserved_chars = {"(", ")", " ", ";", "-", "{", "}"} #, "_"

def LQ(val):
    if not val:
        return QS("")
    if any(c in reserved_chars for c in val):
        return QS(val)
    return LA(val)

def LV(val, nd=1):
    if (val % 1000) == 0:
        return LA("%d" % (val//1000,))
    fmt="%%0.%df" % nd
    return LA(fmt % (val/1000,))
