/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.gui.graph.controller;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.cristalise.gui.graph.view.EditorModeListener;
import org.cristalise.gui.graph.view.EditorToolBar;
import org.cristalise.gui.graph.view.GraphPanel;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;



public class EdgeConstructionController extends MouseAdapter implements EditorModeListener
{
    private GraphModelManager mGraphModelManager = null;
    private GraphPanel 		  mGraphPanel		 = null;
    private EditorToolBar     mEditorToolBar     = null;


    /**********/
    /* States */
    /**********/
    private final Integer kOtherMode    = new Integer(0);
    private final Integer kWaitOrigin   = new Integer(1);
    private final Integer kWaitTerminus = new Integer(2);


    /**********/
    /* Events */
    /**********/
    private final int kEdgeEntered       = 0;
    private final int kOtherEntered      = 1;
    private final int kPressOnVertex     = 2;
    private final int kDrag              = 3;
    private final int kReleaseOnTerminus = 4;
    private final int kReleaseOnNothing  = 5;


    /***********/
    /* Actions */
    /***********/

    private interface Action
    {
        public void doIt(Object data);
    }


    private Action mSetOriginVertex = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            if(mGraphModelManager != null)
            {
                mGraphModelManager.getModel().setNewEdgeOriginVertex((Vertex)data);
            }
        }
    };


    private Action mSetEndPoint = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            if(mGraphModelManager != null)
            {
            	Point mouse = (Point)data;
                mGraphModelManager.getModel().setNewEdgeEndPoint(new GraphPoint(mouse.x, mouse.y));
            }
        }
    };


    private Action mClearEdge = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            if(mGraphModelManager != null)
            {
                mGraphModelManager.getModel().setNewEdgeOriginVertex(null);
                mGraphModelManager.getModel().setNewEdgeEndPoint(null);
            }
        }
    };


    private Action mCreateEdge = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            if((mGraphModelManager != null) && (mEditorToolBar != null) && mGraphModelManager.isEditable())
            {
                mGraphModelManager.getModel().createDirectedEdge
                (
                    mGraphModelManager.getModel().getNewEdgeOriginVertex(),
                    (Vertex)data,
                    mEditorToolBar.getSelectedEdgeType()
                );
                mGraphModelManager.getModel().setNewEdgeOriginVertex(null);
                mGraphModelManager.getModel().setNewEdgeEndPoint(null);
            }
        }
    };


    /***********************************/
    /* Finite State Transition Network */
    /***********************************/

    private Object[][][] mFSTN =
    {//                       OtherMode             WaitOrigin                        WaitTerminus
     /* EdgeEntered       */ {{null, kWaitOrigin},  null                            , null                       },
     /* OtherEntered      */ { null              , {null            , kOtherMode}   , null                       },
     /* PressOnVertex     */ { null              , {mSetOriginVertex, kWaitTerminus}, null                       },
     /* Drag              */ { null              ,  null                            , {mSetEndPoint, null}       },
     /* ReleaseOnTerminus */ { null              ,  null                            , {mCreateEdge , kWaitOrigin}},
     /* ReleaseOnNothing  */ { null              ,  null                            , {mClearEdge  , kWaitOrigin}}
    };


    /*****************/
    /* Current state */
    /*****************/

    private Integer mState = kOtherMode;


    /**************************/
    /* Event processing logic */
    /**************************/

    private void processEvent(int event, Object data)
    {
        Object[] transition = mFSTN[event][mState.intValue()];
        Action   action     = null;
        Integer  nextState  = null;

        if(transition != null)
        {
            action    = (Action)transition[0];
            nextState = (Integer)transition[1];

            if(action != null)
            {
                action.doIt(data);
            }

            if(nextState != null)
            {
                mState = nextState;
            }
        }
    }


    /********************/
    /* Public interface */
    /********************/

    public void setGraphModelManager(GraphModelManager graphModelManager)
    {
        mGraphModelManager = graphModelManager;
    }
    
    public void setGraphPanel(GraphPanel graphPanel)
    {
        mGraphPanel = graphPanel;
    }


    public void setEditorToolBar(EditorToolBar editorToolBar)
    {
        mEditorToolBar = editorToolBar;
        mEditorToolBar.addEditorModeListener(this);
    }


    @Override
	public void editorModeChanged(String idOfNewMode)
    {
        if(idOfNewMode.equals("Edge"))
        {
            processEvent(kEdgeEntered, null);
        }
        else
        {
            processEvent(kOtherEntered, null);
        }
    }


    @Override
	public void mousePressed(MouseEvent me)
    {
        Vertex vertex     = null;
        Point  mousePoint = null;

        if(mGraphModelManager != null && mGraphPanel != null)
        {
            // Determine if there is a vertex under the mouse cursor
            mousePoint = me.getPoint();
            vertex     = mGraphPanel.getVertex(new GraphPoint(mousePoint.x, mousePoint.y));

            // If the mouse has been pressed on a vertex
            if(vertex != null)
            {
                processEvent(kPressOnVertex, vertex);
            }
        }
    }


    @Override
	public void mouseReleased(MouseEvent me)
    {
        Vertex vertex     = null;
        Point  mousePoint = null;

        if(mGraphModelManager != null && mGraphPanel != null)
        {
            // Determine if there is a vertex under the mouse cursor
            mousePoint = me.getPoint();
            vertex     = mGraphPanel.getVertex(new GraphPoint(mousePoint.x, mousePoint.y));

            // If the mouse has been released on a vertex which is not the origin vertex
            if((vertex != null) && (vertex != mGraphModelManager.getModel().getNewEdgeOriginVertex()))
            {
                processEvent(kReleaseOnTerminus, vertex);
            }
            else
            {
                processEvent(kReleaseOnNothing, null);
            }
        }
    }


    @Override
	public void mouseExited(MouseEvent me)
    {
    }


    @Override
	public void mouseDragged(MouseEvent me)
    {
        processEvent(kDrag, me.getPoint());
    }


    @Override
	public void mouseMoved(MouseEvent me)
    {
    }
}
