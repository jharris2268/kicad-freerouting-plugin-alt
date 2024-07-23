# -*- coding: utf-8 -*-

###########################################################################
## Python code generated with wxFormBuilder (version 4.2.1-5-gc2f65a65)
## http://www.wxformbuilder.org/
##
## PLEASE DO *NOT* EDIT THIS FILE!
###########################################################################

import wx
import wx.xrc

import gettext
_ = gettext.gettext

###########################################################################
## Class FreeroutingAltBase
###########################################################################

class FreeroutingAltBase ( wx.Dialog ):

    def __init__( self, parent ):
        wx.Dialog.__init__ ( self, parent, id = wx.ID_ANY, title = _(u"Freerouting Alt"), pos = wx.DefaultPosition, size = wx.Size( 800,720 ), style = wx.DEFAULT_DIALOG_STYLE|wx.DIALOG_NO_PARENT|wx.RESIZE_BORDER )

        self.SetSizeHints( wx.Size( 800,720 ), wx.DefaultSize )

        bSizer1 = wx.BoxSizer( wx.VERTICAL )

        fgSizer1 = wx.FlexGridSizer( 0, 3, 0, 0 )
        fgSizer1.AddGrowableCol( 1 )
        fgSizer1.SetFlexibleDirection( wx.BOTH )
        fgSizer1.SetNonFlexibleGrowMode( wx.FLEX_GROWMODE_NONE )

        self.m_staticText1 = wx.StaticText( self, wx.ID_ANY, _(u"Fanout"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText1.Wrap( -1 )

        fgSizer1.Add( self.m_staticText1, 0, wx.ALL, 5 )

        self.fanout_checkbox = wx.CheckBox( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        fgSizer1.Add( self.fanout_checkbox, 0, wx.ALL, 5 )


        fgSizer1.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.m_staticText3 = wx.StaticText( self, wx.ID_ANY, _(u"Autoroute"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText3.Wrap( -1 )

        fgSizer1.Add( self.m_staticText3, 0, wx.ALL, 5 )

        self.autoroute_checkbox = wx.CheckBox( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.autoroute_checkbox.SetValue(True)
        fgSizer1.Add( self.autoroute_checkbox, 0, wx.ALL, 5 )


        fgSizer1.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.m_staticText4 = wx.StaticText( self, wx.ID_ANY, _(u"Optimize"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText4.Wrap( -1 )

        fgSizer1.Add( self.m_staticText4, 0, wx.ALL, 5 )

        optimize_comboboxChoices = []
        self.optimize_combobox = wx.ComboBox( self, wx.ID_ANY, _(u"0"), wx.DefaultPosition, wx.DefaultSize, optimize_comboboxChoices, 0 )
        fgSizer1.Add( self.optimize_combobox, 0, wx.ALL, 5 )


        fgSizer1.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.m_staticText22 = wx.StaticText( self, wx.ID_ANY, _(u"Route within zones"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText22.Wrap( -1 )

        fgSizer1.Add( self.m_staticText22, 0, wx.ALL, 5 )

        self.route_within_zones_checkbox = wx.CheckBox( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.route_within_zones_checkbox.SetValue(True)
        fgSizer1.Add( self.route_within_zones_checkbox, 0, wx.ALL, 5 )


        fgSizer1.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.m_staticText23 = wx.StaticText( self, wx.ID_ANY, _(u"Only route selected"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText23.Wrap( -1 )

        fgSizer1.Add( self.m_staticText23, 0, wx.ALL, 5 )

        self.only_route_selected_checkbox = wx.CheckBox( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        fgSizer1.Add( self.only_route_selected_checkbox, 0, wx.ALL, 5 )

        self.save_dsn_button = wx.Button( self, wx.ID_ANY, _(u"Save DSN file"), wx.DefaultPosition, wx.DefaultSize, 0 )
        fgSizer1.Add( self.save_dsn_button, 0, wx.ALL, 5 )

        self.m_staticText5 = wx.StaticText( self, wx.ID_ANY, _(u"Progress"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText5.Wrap( -1 )

        fgSizer1.Add( self.m_staticText5, 0, wx.ALL, 5 )

        self.progress_text = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.progress_text.Wrap( -1 )

        fgSizer1.Add( self.progress_text, 0, wx.ALL|wx.EXPAND, 5 )


        fgSizer1.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.m_staticText25 = wx.StaticText( self, wx.ID_ANY, _(u"Info"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText25.Wrap( -1 )

        fgSizer1.Add( self.m_staticText25, 0, wx.ALL, 5 )

        self.info_text = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.info_text.Wrap( -1 )

        fgSizer1.Add( self.info_text, 0, wx.ALL|wx.EXPAND, 5 )


        bSizer1.Add( fgSizer1, 1, wx.ALIGN_LEFT|wx.ALIGN_TOP|wx.EXPAND, 5 )

        self.logging_text = wx.TextCtrl( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.TE_DONTWRAP|wx.TE_MULTILINE )
        self.logging_text.SetFont( wx.Font( 8, wx.FONTFAMILY_TELETYPE, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_NORMAL, False, wx.EmptyString ) )
        self.logging_text.SetMinSize( wx.Size( 800,300 ) )

        bSizer1.Add( self.logging_text, 0, wx.ALL|wx.EXPAND, 5 )

        bSizer3 = wx.BoxSizer( wx.HORIZONTAL )

        fgSizer2 = wx.FlexGridSizer( 0, 7, 0, 0 )
        fgSizer2.SetFlexibleDirection( wx.BOTH )
        fgSizer2.SetNonFlexibleGrowMode( wx.FLEX_GROWMODE_SPECIFIED )

        self.m_staticText6 = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText6.Wrap( -1 )

        fgSizer2.Add( self.m_staticText6, 0, wx.ALL, 5 )

        self.m_staticText7 = wx.StaticText( self, wx.ID_ANY, _(u"Pads"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText7.Wrap( -1 )

        fgSizer2.Add( self.m_staticText7, 0, wx.ALL, 5 )

        self.m_staticText8 = wx.StaticText( self, wx.ID_ANY, _(u"Vias"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText8.Wrap( -1 )

        fgSizer2.Add( self.m_staticText8, 0, wx.ALL, 5 )

        self.m_staticText9 = wx.StaticText( self, wx.ID_ANY, _(u"Track Segments"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText9.Wrap( -1 )

        fgSizer2.Add( self.m_staticText9, 0, wx.ALL, 5 )

        self.m_staticText10 = wx.StaticText( self, wx.ID_ANY, _(u"Nets"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText10.Wrap( -1 )

        fgSizer2.Add( self.m_staticText10, 0, wx.ALL, 5 )

        self.m_staticText11 = wx.StaticText( self, wx.ID_ANY, _(u"Unrouted"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText11.Wrap( -1 )

        fgSizer2.Add( self.m_staticText11, 0, wx.ALL, 5 )

        self.total_length_label1 = wx.StaticText( self, wx.ID_ANY, _(u"Total Length"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.total_length_label1.Wrap( -1 )

        fgSizer2.Add( self.total_length_label1, 0, wx.ALL, 5 )

        self.selected_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.selected_label.Wrap( -1 )

        self.selected_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.selected_label, 0, wx.ALIGN_LEFT, 5 )

        self.pads_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.pads_label.Wrap( -1 )

        self.pads_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.pads_label, 0, wx.ALIGN_LEFT, 5 )

        self.vias_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.vias_label.Wrap( -1 )

        self.vias_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.vias_label, 0, wx.ALIGN_LEFT, 5 )

        self.track_segments_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.track_segments_label.Wrap( -1 )

        self.track_segments_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.track_segments_label, 0, wx.ALIGN_LEFT, 5 )

        self.nets_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.nets_label.Wrap( -1 )

        self.nets_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.nets_label, 0, wx.ALIGN_LEFT, 5 )

        self.unrouted_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.unrouted_label.Wrap( -1 )

        self.unrouted_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.unrouted_label, 0, wx.ALIGN_LEFT, 5 )

        self.total_length_label = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.ALIGN_RIGHT )
        self.total_length_label.Wrap( -1 )

        self.total_length_label.SetMinSize( wx.Size( 70,-1 ) )

        fgSizer2.Add( self.total_length_label, 0, wx.ALIGN_LEFT, 5 )

        self.m_staticText21 = wx.StaticText( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, 0 )
        self.m_staticText21.Wrap( -1 )

        fgSizer2.Add( self.m_staticText21, 0, wx.ALL, 5 )


        bSizer3.Add( fgSizer2, 1, wx.ALIGN_LEFT, 5 )

        self.save_log_button = wx.Button( self, wx.ID_ANY, _(u"Save Log"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.save_log_button.Enable( False )

        bSizer3.Add( self.save_log_button, 0, wx.ALL, 5 )


        bSizer1.Add( bSizer3, 1, wx.EXPAND, 5 )

        bSizer2 = wx.BoxSizer( wx.HORIZONTAL )


        bSizer2.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.close_button = wx.Button( self, wx.ID_CANCEL, _(u"Close"), wx.DefaultPosition, wx.DefaultSize, 0 )

        self.close_button.SetDefault()
        bSizer2.Add( self.close_button, 0, wx.ALL, 5 )

        self.run_button = wx.Button( self, wx.ID_ANY, _(u"Run"), wx.DefaultPosition, wx.DefaultSize, 0 )
        bSizer2.Add( self.run_button, 0, wx.ALL, 5 )


        bSizer2.Add( ( 0, 0), 1, wx.EXPAND, 5 )

        self.revert_button = wx.Button( self, wx.ID_ANY, _(u"Revert"), wx.DefaultPosition, wx.DefaultSize, 0 )
        self.revert_button.Enable( False )

        bSizer2.Add( self.revert_button, 0, wx.ALL, 5 )


        bSizer1.Add( bSizer2, 1, wx.ALIGN_CENTER_HORIZONTAL, 5 )


        self.SetSizer( bSizer1 )
        self.Layout()

    def __del__( self ):
        pass


