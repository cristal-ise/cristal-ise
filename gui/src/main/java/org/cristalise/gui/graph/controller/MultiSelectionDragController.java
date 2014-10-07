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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.cristalise.gui.graph.view.EditorModeListener;
import org.cristalise.gui.graph.view.GraphPanel;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;



public class MultiSelectionDragController
extends MouseAdapter
implements EditorModeListener, KeyListener
{

	private class ResizeInf
    {
        public int          mMousePressedX  = 0;
        public int          mMousePressedY  = 0;
        public Vertex       mSelectedVertex = null;
        public GraphPoint[] mOldOutline     = null;
        public int          mCentreX        = 0;
        public int          mCentreY        = 0;
        public int          mOldHeight      = 0;
        public int          mOldWidth       = 0;
    }
    private ResizeInf mResizeInf = new ResizeInf();

    private class DispForSelection
    {
        public int mXDisp  = 0;
        public int mYDisp  = 0;
    }
    private DispForSelection mDispForSelection = null;

    private class VertexAndDisp
    {
        public Vertex mVertex = null;
        public int    mXDisp  = 0;
        public int    mYDisp  = 0;
    }

    protected GraphModelManager mGraphModelManager = null;
    private GraphPanel mGraphPanel = null;


    /**********/
    /* States */
    /**********/
    protected final Integer kOtherMode         = new Integer(0);
    protected final Integer kWaiting           = new Integer(1);
    protected final Integer kResizing          = new Integer(2);
    protected final Integer kDraggingSelection = new Integer(3);
    protected final Integer kStretching        = new Integer(4);


    /**********/
    /* Events */
    /**********/
    protected final int kSelectEntered     =  0;
    protected final int kOtherEntered      =  1;
    protected final int kPressOnEdge       =  2;
    protected final int kPressOnVertex     =  3;
    protected final int kPressOnSelection  =  4;
    protected final int kPressOnResizePad  =  5;
    protected final int kCTRLPressOnVertex =  6;
    protected final int kCTRL_A            =  7;
    protected final int kPressOnNothing    =  8;
    protected final int kDrag              =  9;
    protected final int kRelease           = 10;
    protected final int kZoomIn            = 11;

    /***********/
    /* Actions */
    /***********/

    protected interface Action
    {
        public void doIt(Object data);
    }


    protected Action mStoreResizeInf = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            Point      mousePoint = (Point)data;
            GraphPoint centre     = null;

            mResizeInf.mMousePressedX  = mousePoint.x;
            mResizeInf.mMousePressedY  = mousePoint.y;
            mResizeInf.mSelectedVertex = mGraphPanel.getSelection().mVertices[0];
            mResizeInf.mOldOutline     = mResizeInf.mSelectedVertex.getOutlinePoints();
            centre                     = mResizeInf.mSelectedVertex.getCentrePoint();
            mResizeInf.mCentreX        = centre.x;
            mResizeInf.mCentreY        = centre.y;
            mResizeInf.mOldHeight      = mResizeInf.mSelectedVertex.getHeight();
            mResizeInf.mOldWidth       = mResizeInf.mSelectedVertex.getWidth();
        }
    };


    protected Action mResizeVertex = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            Point mousePoint = (Point)data;
            int   resizeX    = 0;
            int   resizeY    = 0;


            // Calculate how much the old outline should be resized
            resizeX = mousePoint.x - mResizeInf.mMousePressedX;
            resizeY = mousePoint.y - mResizeInf.mMousePressedY;

            // Clip the resize so that outline does not get any
            // smaller than 10 x 10 pixels
            if(resizeX < -mResizeInf.mOldWidth/2 + 10)
            {
                resizeX = -mResizeInf.mOldWidth/2 + 10;
            }

            if(resizeY < -mResizeInf.mOldHeight/2 + 10)
            {
                resizeY = -mResizeInf.mOldHeight/2 + 10;
            }

            if (mGraphModelManager.isEditable()) {
                mResizeInf.mSelectedVertex.setOutlinePoints(newOutline(resizeX, resizeY));
                mGraphModelManager.forceNotify();
            }
        }


        private GraphPoint[] newOutline(int resizeX, int resizeY)
        {
            GraphPoint[] newOutline = new GraphPoint[mResizeInf.mOldOutline.length];
            int          x          = 0;
            int          y          = 0;
            int          i          = 0;


            for(i=0; i<newOutline.length; i++)
            {
                if(mResizeInf.mOldOutline[i].x > mResizeInf.mCentreX)
                {
                    x = mResizeInf.mOldOutline[i].x + resizeX;
                }
                else if(mResizeInf.mOldOutline[i].x < mResizeInf.mCentreX)
                {
                    x = mResizeInf.mOldOutline[i].x - resizeX;
                }
                else
                {
                    x = mResizeInf.mOldOutline[i].x;
                }

                if(mResizeInf.mOldOutline[i].y > mResizeInf.mCentreY)
                {
                    y = mResizeInf.mOldOutline[i].y + resizeY;
                }
                else if(mResizeInf.mOldOutline[i].y < mResizeInf.mCentreY)
                {
                    y = mResizeInf.mOldOutline[i].y - resizeY;
                }
                else
                {
                    y = mResizeInf.mOldOutline[i].y;
                }

                newOutline[i] = new GraphPoint(x, y);
            }

            return newOutline;
        }
    };


    protected Action mSelectEdge = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            Selection selection = new Selection((DirectedEdge)data, null, 0, 0, 0, 0);

            mGraphPanel.setSelection(selection);
        }
    };


    protected Action mSelectVertexAndStoreDisp = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            VertexAndDisp vertexAndDisp = (VertexAndDisp)data;
            GraphPoint    centrePoint   = vertexAndDisp.mVertex.getCentrePoint();
            Selection     selection     = new Selection(null,
                                                        new Vertex[] {vertexAndDisp.mVertex},
                                                        centrePoint.x,
                                                        centrePoint.y,
                                                        centrePoint.x,
                                                        centrePoint.y);

            mGraphPanel.setSelection(selection);
            mDispForSelection        = new DispForSelection();
            mDispForSelection.mXDisp = vertexAndDisp.mXDisp;
            mDispForSelection.mYDisp = vertexAndDisp.mYDisp;
        }
    };


    protected Action mStoreDisp = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            mDispForSelection = (DispForSelection)data;
        }
    };


    protected Action mToggleVertex = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            Vertex vertex = (Vertex)data;

            if(mGraphPanel.inSelection(vertex))
            {
            	mGraphPanel.removeFromSelection(vertex);
            }
            else
            {
            	mGraphPanel.addToSelection(vertex);
            }
        }
    };


    protected Action mSelectAll = new Action()
    {
        @Override
		public void doIt(Object data)
        {
        	mGraphPanel.selectAll();
        }
    };


    protected Action mCreateBand = new Action()
    {
        @Override
		public void doIt(Object data)
        {
        	Point mouse = (Point)data;
            GraphPoint fixedCorner = new GraphPoint(mouse.x, mouse.y);

            mGraphPanel.setElasticBand(new ElasticBand(fixedCorner, fixedCorner));
        }
    };


    protected Action mMoveSelection = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            Point mousePoint = (Point)data;
            int   topLeftX   = mousePoint.x - mDispForSelection.mXDisp;
            int   topLeftY   = mousePoint.y - mDispForSelection.mYDisp;
            if (mGraphModelManager.isEditable()) {
                mGraphPanel.moveAbsoluteSelection(topLeftX, topLeftY);
            }
        }
    };


    protected Action mResizeBand = new Action()
    {
        @Override
		public void doIt(Object data)
        {
        	Point mouse = (Point)data;
            mGraphPanel.resizeElasticBand(new GraphPoint(mouse.x, mouse.y));
        }
    };


    protected Action mSelectContents = new Action()
    {
        @Override
		public void doIt(Object data)
        {
            mGraphPanel.selectContentsOfElasticBand();
        }
    };

    protected Action mZoomIn = new Action()
    {
        @Override
		public void doIt(Object data) // data is the clicked vertex
        {
            // Need to get child graph model out of the vertex before we can zoom in
            if (data instanceof Vertex) {
                Vertex zoomTo = (Vertex)data;
                if (((GraphableVertex)zoomTo).getIsComposite()) mGraphModelManager.zoomIn(zoomTo);
            }
        }
    };

    /***********************************/
    /* Finite State Transition Network */
    /***********************************/

    protected Object[][][] mFSTN =
    {//                       OtherMode          Waiting                                          Resizing                   DraggingSelection           Stretching
     /* SelectEntered     */ {{null, kWaiting},  null                                          ,  null                    ,  null                     ,  null                      },
     /* OtherEntered      */ { null           , {null                     , kOtherMode        },  null                    ,  null                     ,  null                      },
     /* PressOnEdge       */ { null           , {mSelectEdge              , null              },  null                    ,  null                     ,  null                      },
     /* PressOnVertex     */ { null           , {mSelectVertexAndStoreDisp, kDraggingSelection},  null                    ,  null                     ,  null                      },
     /* PressOnSelection  */ { null           , {mStoreDisp               , kDraggingSelection},  null                    ,  null                     ,  null                      },
     /* PressOnResizePad  */ { null           , {mStoreResizeInf          , kResizing         },  null                    ,  null                     ,  null                      },
     /* CTRLPressOnVertex */ { null           , {mToggleVertex            , null              },  null                    ,  null                     ,  null                      },
     /* CTRL_A            */ { null           , {mSelectAll               , null              },  null                    ,  null                     ,  null                      },
     /* PressOnNothing    */ { null           , {mCreateBand              , kStretching       },  null                    ,  null                     ,  null                      },
     /* Drag              */ { null           ,  null                                          , {mResizeVertex, null    }, {mMoveSelection, null    }, {mResizeBand    , null    }},
     /* Release           */ { null           ,  null                                          , {null         , kWaiting}, {null          , kWaiting}, {mSelectContents, kWaiting}},
     /* Double Click      */ { null           , {mZoomIn                  , null              },  null                    ,  null                     ,  null                      },
    };


    /*****************/
    /* Current state */
    /*****************/

    private Integer mState = kWaiting;

	/**************************/
    /* Event processing logic */
    /**************************/

    protected void processEvent(int event, Object data)
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


    public MultiSelectionDragController(GraphModelManager graphModelManager,
			GraphPanel graphPanel) {
		mGraphModelManager = graphModelManager;
		mGraphPanel = graphPanel;
	}

    @Override
	public void editorModeChanged(String idOfNewMode)
    {
        if(idOfNewMode.equals("Select"))
        {
            processEvent(kSelectEntered, null);
        }
        else
        {
            processEvent(kOtherEntered, null);
        }
    }


    @Override
	public void mousePressed(MouseEvent me)
    {
        int modifiers = me.getModifiers();

        if(mGraphModelManager != null)
        {
            if((modifiers & InputEvent.CTRL_MASK) == 0)
            {
                mousePressedWithoutCTRL(me.getPoint());
            }
            else
            {
                mousePressedWithCTRL(me.getPoint());
            }
        }
    }

    @Override
	public void mouseClicked(MouseEvent me)
    {
        if (me.getClickCount() == 2) { // want double click
            Point clickedSpot = me.getPoint();
            GraphPoint mouseGPoint = new GraphPoint(clickedSpot.x, clickedSpot.y);
            Vertex clicked = mGraphPanel.getVertex(mouseGPoint);
            if (clicked != null)
                processEvent(kZoomIn, clicked);
        }
    }

    private void mousePressedWithoutCTRL(Point mousePoint)
    {
        GraphPoint       mouseGPoint       = new GraphPoint(mousePoint.x, mousePoint.y);
        DirectedEdge     edge              = mGraphPanel.getEdge(mouseGPoint);
        Vertex           vertex            = mGraphPanel.getVertex(mouseGPoint);
        GraphPoint       vertexCentrePoint = null;
        VertexAndDisp    vertexAndDisp     = null;
        Selection        selection         = null;
        DispForSelection dispForSelection  = null;


        // In order of priority:
        // 1. Click on resize pad
        // 2. Click on vertex
        // 3. Click on edge
        if(onResizePad(mouseGPoint))
        {
            processEvent(kPressOnResizePad, mousePoint);
        }
        else if(vertex != null)
        {
            if(mGraphPanel.inSelection(vertex))
            {
                selection               = mGraphPanel.getSelection();
                dispForSelection        = new DispForSelection();
                dispForSelection.mXDisp = mousePoint.x - selection.mTopLeftX;
                dispForSelection.mYDisp = mousePoint.y - selection.mTopLeftY;

                processEvent(kPressOnSelection, dispForSelection);
            }
            else
            {
                vertexCentrePoint = vertex.getCentrePoint();

                vertexAndDisp         = new VertexAndDisp();
                vertexAndDisp.mVertex = vertex;
                vertexAndDisp.mXDisp  = mousePoint.x - vertexCentrePoint.x;
                vertexAndDisp.mYDisp  = mousePoint.y - vertexCentrePoint.y;

                processEvent(kPressOnVertex, vertexAndDisp);
            }
        }
        // vertex == null
        else
        {
            if(edge == null)
            {
                processEvent(kPressOnNothing, mousePoint);
            }
            else
            {
                processEvent(kPressOnEdge, edge);
            }
        }
    }


    private boolean onResizePad(GraphPoint mouseGPoint)
    {
        Selection  selection    = mGraphPanel.getSelection();
        GraphPoint vertexCentre = null;
        int        bottomRightX = 0;
        int        bottomRightY = 0;


        if(selection.mVertices == null) return false;

        if(selection.mVertices.length == 1)
        {
            vertexCentre = selection.mVertices[0].getCentrePoint();
            if (vertexCentre == null) return false;
            bottomRightX = vertexCentre.x + selection.mVertices[0].getWidth()/2;
            bottomRightY = vertexCentre.y + selection.mVertices[0].getHeight()/2;

            return
            (
                (mouseGPoint.x > bottomRightX)      &&
                (mouseGPoint.x < bottomRightX + 10) &&
                (mouseGPoint.y > bottomRightY)      &&
                (mouseGPoint.y < bottomRightY + 10)
            );
        }
        else
        {
            return false;
        }
    }


    private void mousePressedWithCTRL(Point mousePoint)
    {
        Vertex vertex = mGraphPanel.getVertex(new GraphPoint(mousePoint.x, mousePoint.y));

        if(vertex != null)
        {
            processEvent(kCTRLPressOnVertex, vertex);
        }
    }


    @Override
	public void mouseReleased(MouseEvent me)
    {
        processEvent(kRelease, null);
    }


    @Override
	public void mouseMoved(MouseEvent me)
    {
    }


    @Override
	public void mouseDragged(MouseEvent e)
    {
        processEvent(kDrag, e.getPoint());
    }


    @Override
	public void keyPressed(KeyEvent e)
    {
        if((e.getKeyCode() == KeyEvent.VK_A) && e.isControlDown())
        {
            processEvent(kCTRL_A, null);
        }
    }


    @Override
	public void keyReleased(KeyEvent e)
    {
    }


    @Override
	public void keyTyped(KeyEvent e)
    {
    }

}
