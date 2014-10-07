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
package org.cristalise.gui.graph.view;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JPanel;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.graph.controller.ElasticBand;
import org.cristalise.gui.graph.controller.Selection;
import org.cristalise.gui.graph.event.ElasticBandResizedEvent;
import org.cristalise.gui.graph.event.ElasticBandSetEvent;
import org.cristalise.gui.graph.event.SelectionChangedEvent;
import org.cristalise.gui.graph.event.SelectionMovedEvent;
import org.cristalise.kernel.graph.event.EntireModelChangedEvent;
import org.cristalise.kernel.graph.event.GraphModelResizedEvent;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;

public class GraphPanel extends JPanel implements Observer
{
    // There should always be a Selection object
    protected Selection         mSelection = new Selection(null, null, 0, 0, 0, 0);
    private   ElasticBand       mElasticBand = null;
	protected final Paint mSelectionPaint = Color.black;
	protected final Paint mStartPaint = Color.green;
    protected final Image mResizePadImg = ImageLoader.findImage("graph/resizepad.gif").getImage();
    protected final BasicStroke mDashed =
		new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5.0f }, 0.0f);
	protected GraphModelManager mGraphModelManager = null;
	protected VertexRenderer mVertexRenderer = null;
	protected DirectedEdgeRenderer mDirectedEdgeRenderer = null;
    private final SelectionChangedEvent mSelectionChangedEvent = new SelectionChangedEvent();
    private final SelectionMovedEvent mSelectionMovedEvent = new SelectionMovedEvent();
    private final ElasticBandResizedEvent mElasticBandResizedEvent = new ElasticBandResizedEvent();
    private final ElasticBandSetEvent mElasticBandSetEvent = new ElasticBandSetEvent();
    
	public GraphPanel(DirectedEdgeRenderer eRenderer, VertexRenderer vRenderer)
	{
		mVertexRenderer = vRenderer;
		mDirectedEdgeRenderer = eRenderer;
		// Request the keyboard focus if the mouse
		// is pressed on the graph panel
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent me)
			{
				requestFocus();
			}
		});
	}
	public void setGraphModelManager(GraphModelManager graphModelManager)
	{
		mGraphModelManager = graphModelManager;
	}
	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof EntireModelChangedEvent)
		{
	        mSelection           = new Selection(null, null, 0, 0, 0, 0);
		}
		if (arg instanceof GraphModelResizedEvent || arg instanceof EntireModelChangedEvent)
		{
			setPreferredSize(new Dimension(mGraphModelManager.getModel().getWidth(), mGraphModelManager.getModel().getHeight()));
			revalidate();
		}
		repaint();
	}
    // Updates the top left and bottom right corners of the selection
    private void updateSelectionCorners() {
        Vertex vertex = mSelection.mVertices[0];
        GraphPoint centrePoint = vertex.getCentrePoint();
        if (centrePoint == null) return;
        mSelection.mTopLeftX = centrePoint.x;
        mSelection.mTopLeftY = centrePoint.y;
        mSelection.mBottomRightX = centrePoint.x;
        mSelection.mBottomRightY = centrePoint.y;
        for (Vertex mVertice : mSelection.mVertices) {
            vertex = mVertice;
            centrePoint = vertex.getCentrePoint();
            if (centrePoint.x < mSelection.mTopLeftX) {
                mSelection.mTopLeftX = centrePoint.x;
            }
            if (centrePoint.y < mSelection.mTopLeftY) {
                mSelection.mTopLeftY = centrePoint.y;
            }
            if (centrePoint.x > mSelection.mBottomRightX) {
                mSelection.mBottomRightX = centrePoint.x;
            }
            if (centrePoint.y > mSelection.mBottomRightY) {
                mSelection.mBottomRightY = centrePoint.y;
            }
        }
    }

    public void deleteSelection() {
        int i = 0;
        if (mSelection.mVertices != null) {
            for (i = 0; i < mSelection.mVertices.length; i++) {
            	mGraphModelManager.getModel().removeVertex(mSelection.mVertices[i]);
            }
        }
        else if (mSelection.mEdge != null) {
        	mGraphModelManager.getModel().removeEdge(mSelection.mEdge);
        }
        // Make sure nothing is selected
        if ((mSelection.mEdge != null) || (mSelection.mVertices != null)) {
            mSelection.mEdge = null;
            mSelection.mVertices = null;
            mSelectionChangedEvent.mSelection = mSelection;
            mGraphModelManager.notifyObservers(mSelectionChangedEvent);
        }
    }

    public void selectAll() {
        Vertex[] allVertices = mGraphModelManager.getModel().getVertices();
        if (allVertices.length > 0) {
            mSelection.mEdge = null;
            mSelection.mVertices = allVertices;
            updateSelectionCorners();
            mSelectionChangedEvent.mSelection = mSelection;
            mGraphModelManager.notifyObservers(mSelectionChangedEvent);
        }
    }

    public void selectContentsOfElasticBand() {
        if (mElasticBand == null) return;
        Polygon bandPolygon = new Polygon();
        Vertex[] allVertices = mGraphModelManager.getModel().getVertices();
        GraphPoint centrePoint = null;
        Vector<Vertex> verticesInside = new Vector<Vertex>(10, 10);
        int i = 0;
        // Create a polygon representing the elastic band
        bandPolygon.addPoint(mElasticBand.mFixedCorner.x, mElasticBand.mFixedCorner.y);
        bandPolygon.addPoint(mElasticBand.mMovingCorner.x, mElasticBand.mFixedCorner.y);
        bandPolygon.addPoint(mElasticBand.mMovingCorner.x, mElasticBand.mMovingCorner.y);
        bandPolygon.addPoint(mElasticBand.mFixedCorner.x, mElasticBand.mMovingCorner.y);
        // Create a vector of all of the vertices within the elastic band polygon
        for (i = 0; i < allVertices.length; i++) {
            centrePoint = allVertices[i].getCentrePoint();
            if (bandPolygon.contains(centrePoint.x, centrePoint.y)) {
                verticesInside.add(allVertices[i]);
            }
        }

        // Select the vertices found within the elastic band polygon
        if (verticesInside.size() == 0) {
            mSelection.mTopLeftX = 0;
            mSelection.mTopLeftY = 0;
            mSelection.mBottomRightX = 0;
            mSelection.mBottomRightY = 0;
            mSelection.mEdge = null;
            // select parent vertex if we have selected the 'paper'
            if (mGraphModelManager.getModel().getContainingVertex() != null)
                verticesInside.add(mGraphModelManager.getModel().getContainingVertex());
            else
                mSelection.mVertices = null;
        }

        if (verticesInside.size() > 0) {
            mSelection.mEdge = null;
            mSelection.mVertices = new Vertex[verticesInside.size()];
            for (i = 0; i < verticesInside.size(); i++) {
                mSelection.mVertices[i] = verticesInside.elementAt(i);
            }
            updateSelectionCorners();
        }
        // Remove the elastic band
        mElasticBand = null;
        mSelectionChangedEvent.mSelection = mSelection;
        mGraphModelManager.notifyObservers(mSelectionChangedEvent);
    }



    public void setSelectedVertexToBeStart() {
        if (mSelection.mVertices != null) {
            if (mSelection.mVertices.length == 1) {
            	mGraphModelManager.getModel().setStartVertexId(mSelection.mVertices[0].getID());
            }
        }
    }
    
    public Selection getSelection() {
    	return mSelection;
    }

    public void setElasticBand(ElasticBand elasticBand) {
        mElasticBand = elasticBand;
        mGraphModelManager.notifyObservers(mElasticBandSetEvent);
    }

    public void resizeElasticBand(GraphPoint movingCorner) {
        mElasticBand.mMovingCorner = movingCorner;
        mGraphModelManager.notifyObservers(mElasticBandResizedEvent);
    }

    public boolean inSelection(Vertex v) {
        int i = 0;
        if (mSelection.mVertices == null) {
            return false;
        }
        else {
            for (i = 0; i < mSelection.mVertices.length; i++) {
                if (mSelection.mVertices[i] == v) {
                    return true;
                }
            }
            return false;
        }
    }

    // Only use this method to remove one vertex.
    // If you wish to remove more, it would
    // propably be more efficient to create a
    // new Selection object.
    public void removeFromSelection(Vertex v) {
        Vertex[] vertices = null;
        int i = 0;
        int j = 0;
        if (mSelection.mVertices.length == 1) {
            mSelection.mVertices = null;
            mSelection.mTopLeftX = 0;
            mSelection.mTopLeftY = 0;
            mSelection.mBottomRightX = 0;
            mSelection.mBottomRightY = 0;
        }
        else {
            vertices = new Vertex[mSelection.mVertices.length - 1];
            for (i = 0; i < mSelection.mVertices.length; i++) {
                if (mSelection.mVertices[i] != v) {
                    vertices[j] = mSelection.mVertices[i];
                    j++;
                }
            }
            mSelection.mVertices = vertices;
            updateSelectionCorners();
        }
        mGraphModelManager.notifyObservers(mSelectionChangedEvent);
    }

    // Only use this method to add one vertex.
    // If you wish to add more, it would
    // propably be more efficient to create a
    // new Selection object.
    public void addToSelection(Vertex v) {
        Vertex[] vertices = new Vertex[mSelection.mVertices.length + 1];
        GraphPoint centrePoint = null;
        int i = 0;
        if (mSelection.mVertices == null) {
            centrePoint = v.getCentrePoint();
            mSelection.mVertices = new Vertex[] { v };
            mSelection.mTopLeftX = centrePoint.x;
            mSelection.mTopLeftY = centrePoint.y;
            mSelection.mBottomRightX = centrePoint.x;
            mSelection.mBottomRightY = centrePoint.y;
        }
        else {
            for (i = 0; i < mSelection.mVertices.length; i++) {
                vertices[i] = mSelection.mVertices[i];
            }
            vertices[mSelection.mVertices.length] = v;
            mSelection.mVertices = vertices;
            updateSelectionCorners();
        }
        mGraphModelManager.notifyObservers(mSelectionChangedEvent);
    }
    public void moveAbsoluteSelection(int newTopLeftX, int newTopLeftY) {
        int selectionHeight = mSelection.mBottomRightY - mSelection.mTopLeftY;
        int selectionWidth = mSelection.mBottomRightX - mSelection.mTopLeftX;
        int bottomRightX = newTopLeftX + selectionWidth;
        int bottomRightY = newTopLeftY + selectionHeight;
        GraphPoint oldCentrePoint = null;
        GraphPoint newCentrePoint = null;
        int distXFromTopLeft = 0;
        int distYFromTopLeft = 0;
        int i = 0;
        // Make sure the selection does not move
        // outside the boundaries of the graph
        if (newTopLeftX < 0) newTopLeftX = 0;
        if (newTopLeftY < 0) newTopLeftY = 0;
        int graphWidth = mGraphModelManager.getModel().getWidth();
        if (bottomRightX > graphWidth) newTopLeftX = graphWidth - selectionWidth;
        int graphHeight = mGraphModelManager.getModel().getHeight();
        if (bottomRightY > graphHeight) newTopLeftY = graphHeight - selectionHeight;
        // For each selected vertex
        for (i = 0; i < mSelection.mVertices.length; i++) {
            // Calculate the new centre point of the vertex.
            // First calculate the distance of the centre point
            // from the old top left hand corner of the selection,
            // then move the point to the new top left hand
            // corner plus the distance.
            oldCentrePoint = mSelection.mVertices[i].getCentrePoint();
            distXFromTopLeft = oldCentrePoint.x - mSelection.mTopLeftX;
            distYFromTopLeft = oldCentrePoint.y - mSelection.mTopLeftY;
            newCentrePoint = new GraphPoint(newTopLeftX + distXFromTopLeft, newTopLeftY + distYFromTopLeft);
            mGraphModelManager.getModel().moveAbsoluteVertex(mSelection.mVertices[i], newCentrePoint);
        }
        // Update the top left and bottom right corners
        mSelection.mTopLeftX = newTopLeftX;
        mSelection.mTopLeftY = newTopLeftY;
        mSelection.mBottomRightX = newTopLeftX + selectionWidth;
        mSelection.mBottomRightY = newTopLeftY + selectionHeight;
        mGraphModelManager.notifyObservers(mSelectionMovedEvent);
    }
    
    // If the specified point is within more than one vertex,
    // then the smallest vertex is returned.
    public Vertex getVertex(GraphPoint p) {
        Object[] vertexObjs = mGraphModelManager.getModel().getVertices();
        Vertex vertex = null;
        Vector<Vertex> vertexVector = new Vector<Vertex>(10, 10);
        int numVerticesFound = 0;
        Vertex smallestVertex = null;
        int sizeOfSmallestVertex = 0;
        int sizeOfVertex = 0;
        int i = 0;
        for (i = 0; i < vertexObjs.length; i++) {
            vertex = (Vertex)vertexObjs[i];
            if (vertex.containsPoint(p)) {
                vertexVector.add(vertex);
            }
        }
        numVerticesFound = vertexVector.size();
        if (numVerticesFound == 0) {
            return null;
        }
        else {
            smallestVertex = vertexVector.elementAt(0);
            sizeOfSmallestVertex = smallestVertex.getHeight() * smallestVertex.getWidth();
            // Determine the smallest vertex
            for (i = 1; i < numVerticesFound; i++) {
                vertex = vertexVector.elementAt(i);
                sizeOfVertex = vertex.getHeight() * vertex.getWidth();
                if (sizeOfVertex < sizeOfSmallestVertex) {
                    smallestVertex = vertex;
                    sizeOfSmallestVertex = sizeOfVertex;
                }
            }
            return smallestVertex;
        }
    }
    
    public DirectedEdge getEdge(GraphPoint p) {
        Object[] edgeObjs = mGraphModelManager.getModel().getEdges();
        DirectedEdge edge = null;
        int i = 0;
        for (i = 0; i < edgeObjs.length; i++) {
            edge = (DirectedEdge)edgeObjs[i];
            if (edge.containsPoint(p)) {
                return edge;
            }
        }
        return null;
    }

    
    public void setSelection(Selection s) {
        // If the there is a change
        if (selectionChanged(s)) {
            mSelection = s;
            mSelectionChangedEvent.mSelection = s;
            mGraphModelManager.notifyObservers(mSelectionChangedEvent);
        }
    }

    private boolean selectionChanged(Selection newValue) {
        int i = 0;
        if (mSelection.mEdge != newValue.mEdge) {
            return true;
        }
        if (mSelection.mVertices == null) {
            if (newValue.mVertices == null) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            if (newValue.mVertices == null) {
                return true;
            }
            else {
                if (mSelection.mVertices.length != newValue.mVertices.length) {
                    return true;
                }
                for (i = 0; i < mSelection.mVertices.length; i++) {
                    if (mSelection.mVertices[i] != newValue.mVertices[i]) {
                        return true;
                    }
                }
                return false;
            }
        }
    }    
	@Override
	public void paintComponent(Graphics g)
	{
        Graphics2D g2d = (Graphics2D) g;
		DirectedEdge[] edges = null;
		Vertex[] vertices = null;
		Vertex startVertex = null;
		Vertex newEdgeOriginVertex = null;
		GraphPoint newEdgeOriginPoint = null;
		GraphPoint newEdgeEndPoint = null;
		GraphPoint vertexCentre = null;
		int i = 0;
		super.paintComponent(g);
		if (mGraphModelManager != null)
		{
			// Get the edges and vertices from the model
			edges = mGraphModelManager.getModel().getEdges();
			vertices = mGraphModelManager.getModel().getVertices();
			//graphable = mGraphModelManager.getModel().
			// Draw the edges
			for (i = 0; i < edges.length; i++)
			{
				mDirectedEdgeRenderer.draw(g2d, edges[i]);
			}
			// Draw the vertices
			for (i = 0; i < vertices.length; i++)
			{
				mVertexRenderer.draw(g2d, vertices[i]);
			}
			g2d.setPaint(mStartPaint);
			// Highlight the start vertex if there is one
			startVertex = mGraphModelManager.getModel().getStartVertex();
			if (startVertex != null)
			{
				drawVertexHighlight(g2d, startVertex, 1);
			}
			g2d.setPaint(mSelectionPaint);
			// Draw the outline of the selected
			// vertices if there are any
			if (mSelection.mVertices != null)
			{
				g2d.setStroke(mDashed);
				for (i = 0; i < mSelection.mVertices.length; i++)
				{
                    if (mSelection.mVertices[i] != mGraphModelManager.getModel().getContainingVertex())
                        drawVertexHighlight(g2d, mSelection.mVertices[i], 5);
				}
				// Draw the resize pads if there is one and only one vertex selected
				if (mSelection.mVertices.length == 1 &&
						mSelection.mVertices[0] != mGraphModelManager.getModel().getContainingVertex())
				{
					vertexCentre = mSelection.mVertices[0].getCentrePoint();
					g2d.drawImage(
						mResizePadImg,
						vertexCentre.x + mSelection.mVertices[0].getWidth() / 2,
						vertexCentre.y + mSelection.mVertices[0].getHeight() / 2,
						this);
				}
			}
			// Draw the outline of the selected
			// edge if there is one
			if (mSelection.mEdge != null)
			{
				drawEdgeHighlight(g2d, mSelection.mEdge);
			}
			// Draw the elastic band if there
			// is one
			if (mElasticBand != null)
			{
				g2d.drawLine(
						mElasticBand.mFixedCorner.x,
						mElasticBand.mFixedCorner.y,
						mElasticBand.mMovingCorner.x,
						mElasticBand.mFixedCorner.y);
				g2d.drawLine(
						mElasticBand.mMovingCorner.x,
						mElasticBand.mFixedCorner.y,
						mElasticBand.mMovingCorner.x,
						mElasticBand.mMovingCorner.y);
				g2d.drawLine(
						mElasticBand.mMovingCorner.x,
						mElasticBand.mMovingCorner.y,
						mElasticBand.mFixedCorner.x,
						mElasticBand.mMovingCorner.y);
				g2d.drawLine(
						mElasticBand.mFixedCorner.x,
						mElasticBand.mMovingCorner.y,
						mElasticBand.mFixedCorner.x,
						mElasticBand.mFixedCorner.y);
			}
			// Draw the new edge under construction if there is one
			newEdgeEndPoint = mGraphModelManager.getModel().getNewEdgeEndPoint();
			newEdgeOriginVertex = mGraphModelManager.getModel().getNewEdgeOriginVertex();
			if ((newEdgeEndPoint != null) && (newEdgeOriginVertex != null))
			{
				newEdgeOriginPoint = newEdgeOriginVertex.getCentrePoint();
				g2d.setPaint(Color.black);
				g2d.drawLine(newEdgeOriginPoint.x, newEdgeOriginPoint.y, newEdgeEndPoint.x, newEdgeEndPoint.y);
			}
		}
	}
	// Draws the highlight of the specified vertex the specified dist from its outline
	protected void drawVertexHighlight(Graphics2D g2d, Vertex vertex, int dist)
	{
		GraphPoint[] outlinePoints = vertex.getOutlinePoints();
		GraphPoint centrePoint = vertex.getCentrePoint();
		int i = 0;
		/*
		 * float dash1[] ={5.0f}; BasicStroke bs = new BasicStroke(5.0f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,10.0f, dash1,0.0f);
		 */
		for (i = 0; i < outlinePoints.length - 1; i++)
		{
			drawShiftedLine(dist, g2d, centrePoint, outlinePoints[i].x, outlinePoints[i].y, outlinePoints[i + 1].x, outlinePoints[i + 1].y);
		}
		drawShiftedLine(
			dist,
			g2d,
			centrePoint,
			outlinePoints[outlinePoints.length - 1].x,
			outlinePoints[outlinePoints.length - 1].y,
			outlinePoints[0].x,
			outlinePoints[0].y);
	}
	// Draws the specifed line the specified distance away from the specified centre point
	private static void drawShiftedLine(int dist, Graphics2D g2d, GraphPoint centrePoint, int x1, int y1, int x2, int y2)
	{
		if (x1 > centrePoint.x)
			x1 += dist;
		if (x1 < centrePoint.x)
			x1 -= dist;
		if (y1 > centrePoint.y)
			y1 += dist;
		if (y1 < centrePoint.y)
			y1 -= dist;
		if (x2 > centrePoint.x)
			x2 += dist;
		if (x2 < centrePoint.x)
			x2 -= dist;
		if (y2 > centrePoint.y)
			y2 += dist;
		if (y2 < centrePoint.y)
			y2 -= dist;
		g2d.drawLine(x1, y1, x2, y2);
	}
	// Draws the highlight of the specified edge
	protected void drawEdgeHighlight(Graphics2D g2d, DirectedEdge edge)
	{
		GraphPoint originPoint = edge.getOriginPoint();
		GraphPoint terminusPoint = edge.getTerminusPoint();
		int midX = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
		int midY = originPoint.y + (terminusPoint.y - originPoint.y) / 2;
		int minX = midX - 10;
		int minY = midY - 10;
		int maxX = midX + 10;
		int maxY = midY + 10;
		g2d.drawLine(minX, minY, maxX, minY);
		g2d.drawLine(maxX, minY, maxX, maxY);
		g2d.drawLine(maxX, maxY, minX, maxY);
		g2d.drawLine(minX, maxY, minX, minY);
	}
	@Override
	public void printComponent(Graphics g)
	{
        super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		DirectedEdge[] edges = null;
		Vertex[] vertices = null;
		Vertex startVertex = null;
		int i = 0;
		g.setColor(Color.white);
		g2d.fillRect(0,0,getWidth(),getHeight());
		if (mGraphModelManager != null)
		{
			// Get the edges and vertices from the model
			edges = mGraphModelManager.getModel().getEdges();
			vertices = mGraphModelManager.getModel().getVertices();
			//graphable = mGraphModelManager.getModel().
			// Draw the edges
			for (i = 0; i < edges.length; i++)
			{
				mDirectedEdgeRenderer.draw(g2d, edges[i]);
			}
			// Draw the vertices
			for (i = 0; i < vertices.length; i++)
			{
				mVertexRenderer.draw(g2d, vertices[i]);
			}
			g2d.setPaint(mStartPaint);
			// Highlight the start vertex if there is one
			startVertex = mGraphModelManager.getModel().getStartVertex();
			if (startVertex != null)
			{
				drawVertexHighlight(g2d, startVertex, 1);
			}
		}
	}

    protected void superPaint(Graphics g)
    {
        super.paintComponent(g);
    }
}
