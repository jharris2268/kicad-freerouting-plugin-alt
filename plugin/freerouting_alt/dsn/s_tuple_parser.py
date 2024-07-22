# 
# This file is part of the kicad-freerouting-plugin-alt distribution
# (https://github.com/jharris2268/kicad-freerouting-plugin-alt), a freerouting plugin for the Kicad
# EDA CAD application.
# 
# Copyright (c) 2024 James Harris.
# Copyright (C) 2024 KiCad Developers,
#   see https://github.com/KiCad/kicad-source-mirror/blob/master/AUTHORS.txt for contributors.
#
# This program is free software: you can redistribute it and/or modify  
# it under the terms of the GNU General Public License as published by  
# the Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# This program is distributed in the hope that it will be useful, but 
# WITHOUT ANY WARRANTY; without even the implied warranty of 
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License 
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

ALL_WHITESPACE_EQUAL=True

class Tuple:
    type='Tuple'
    def __init__(self, vals):
        self.vals = vals
    
    def __str__(self):
        return "(%s)" % "".join(str(v) for v in self.vals)
    
    def __repr__(self):
        return "(%s... [%d %d])" % (self.vals[0], len(self.vals), len(str(self)))
    #def __repr__(self):
    #    return "(\n%s\n)" % "\n".join("  %s" % (
    #            repr(v) if len(repr(v))<100 else '%.100s... [%d]' % (repr(v),len(repr(v)))
    #        ) for v in self.vals if not isinstance(v, Whitespace))
    
    @property
    def non_ws(self):
        return [v for v in self.vals if not isinstance(v, Whitespace)]
    
    def find(self, label):
        res=[]
        for v in self.vals:
            if isinstance(v, Tuple):
                if isinstance(v.vals[0], Label) and v.vals[0].val==label:
                    res.append(v)
        return res
        
    def __eq__(self, other):
        return self.type==other.type and self.vals==other.vals
    
    def __hash__(self):
        return hash((self.type,tuple(self.vals)))
    
class Whitespace:
    type='Whitespace'
    def __init__(self, val):
        self.val=val
    
    def __str__(self):
        return self.val
    
    def __repr__(self):
        return repr(str(self))
    
    def __eq__(self, other):
        return self.type==other.type and (self.val==other.val or ALL_WHITESPACE_EQUAL)
    
    def __hash__(self):
        return hash(self.type) if ALL_WHITESPACE_EQUAL else hash((self.type,self.val))
        
class Label:
    type='Label'
    def __init__(self, val):
        self.val=val
    
    def __str__(self):
        return self.val
        
    def __repr__(self):
        return repr(str(self))
    
    def __eq__(self, other):
        return self.type==other.type and self.val==other.val
    
    def __hash__(self):
        return hash((self.type,self.val))
        
class QuotedString:
    type='QuotedString'
    def __init__(self, val):
        self.val=val
    
    def __str__(self):
        return "\"%s\"" % self.val
    
    def __repr__(self):
        return repr(str(self))
    
    def __eq__(self, other):
        return self.type==other.type and self.val==other.val

    def __hash__(self):
        return hash((self.type,self.val))

def read_tuple(input_str, idx):
    if input_str[idx]!='(':
        raise Exception("expected '(' at %d" % idx)
    idx+=1
    result = []
    while True:
        if idx >= len(input_str):
            raise Exception("at end??")

        if input_str[idx].isspace():
            val, idx = read_whitespace(input_str, idx)
            result.append(val)            
            
        elif input_str[idx]=='"':
            
            val,idx = read_quoted_string(input_str, idx)
            result.append(val)
        elif input_str[idx] == '(':

            curr, idx = read_tuple(input_str, idx)
            result.append(curr)
        elif input_str[idx] == ')':
            return Tuple(result), idx+1
        else:
            curr,idx = read_label(input_str, idx)
            result.append(curr)
            
            #check for special case (quoted_string ")
            if curr==Label("string_quote"):
                if input_str[idx:idx+3] == ' ")':
                    result.append(Whitespace(" "))
                    result.append(Label('"'))
                    idx+=2
        
            
                        

def read_whitespace(input_str, idx):
    lp=idx
    while lp<len(input_str) and input_str[lp].isspace():
        lp+=1
    return Whitespace(input_str[idx:lp]), lp
    
    
def read_label(input_str, idx):
    lp = idx
    while not (input_str[lp] in ('(',')') or input_str[lp].isspace()):
        lp+=1
       
    return Label(input_str[idx:lp]), lp

def read_quoted_string(input_str, idx):
    if not input_str[idx] == '"':
        raise Exception("expected '\"' at %d" % idx)
    lp=idx+1
    is_can=False
    while is_can or input_str[lp]!='"':
        is_can = input_str[lp]=='\\'
        lp+=1
    return QuotedString(input_str[idx+1:lp]), lp+1

def read_all(input_file):
    input_str = open(input_file).read()
    
    result=[]
    idx=0
    while idx < len(input_str):
        if input_str[idx]=='(':
            val, idx = read_tuple(input_str, idx)
            result.append(val)
        elif input_str[idx].isspace():
            val,idx = read_whitespace(input_str, idx)
            result.append(val)
        else:
            raise Exception("expected '(' or space at %d" % idx)
    return result
