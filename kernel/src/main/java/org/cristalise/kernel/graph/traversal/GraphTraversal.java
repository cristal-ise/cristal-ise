/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.graph.traversal;


import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.Vertex;



public class GraphTraversal
{
    public static final int kUp   = 1;
    public static final int kDown = 2;


    private GraphTraversal()
    {
    }


    public static Vertex[] getTraversal(GraphModel graphModel, Vertex startVertex, int direction, boolean ignoreBackLinks)
    {
        Vector<Vertex> path = new Vector<Vertex>(10, 10);

        graphModel.clearTags(startVertex);
        visitVertex(startVertex, graphModel, path, direction, startVertex, ignoreBackLinks);

        return path.toArray(new Vertex[path.size()]);
    }


    private static void visitVertex(Vertex vertex, GraphModel graphModel, Vector<Vertex> path, int direction, Object tag, boolean ignoreBackLinks)
    {
        Vertex[] children = null;
        int i = 0;

        if(direction == kDown)
        {
            children = graphModel.getOutVertices(vertex);
        }
        else
        {
            children = graphModel.getInVertices(vertex);
        }

        vertex.setTag(tag);
        path.add(vertex);

        for(i=0; i<children.length; i++)
        {
            if(!(children[i].hasTag(tag)))
            {
                boolean skipBackLink = false;
                if ( ignoreBackLinks &&
                    ((vertex.isJoin() && direction == kUp) ||
                     (vertex.isLoop() && direction == kDown)))
                {
                    Vertex[] following = getTraversal(graphModel, children[i], direction, false);
                    for (Vertex element : following) {
                        if (element == vertex) {
                            skipBackLink = true;
                            break;
                        }
                    }
                }
                if (!skipBackLink)
                    visitVertex(children[i], graphModel, path, direction, tag, ignoreBackLinks);
            }
        }
    }



    public static Vertex[] getTraversal(GraphModel graphModel, Vertex startVertex, Vertex endVertex, int direction, boolean ignoreBackLinks) {
        Set<Vertex> path = new LinkedHashSet<>();

        graphModel.clearTags(startVertex);
        visitVertex(startVertex, endVertex, graphModel, path, direction, startVertex, ignoreBackLinks);

        return path.toArray(new Vertex[path.size()]);
    }

    private static void visitVertex(Vertex startVertex, Vertex endVertex, GraphModel graphModel, Set<Vertex> path, int direction, Object tag, boolean ignoreBackLinks) {
        Vertex[] siblings = null;

        if(direction == kDown) siblings = graphModel.getOutVertices(startVertex);
        else                   siblings = graphModel.getInVertices(startVertex);

        startVertex.setTag(tag);
        path.add(startVertex);

        for(int i = 0; i < siblings.length; i++) {
            //visit until it is tagged or we found the endVertex
            if(! siblings[i].hasTag(tag) && ! siblings[i].equals(endVertex)) {
                boolean skipBackLink = false;

                if ( ignoreBackLinks &&
                    ((startVertex.isJoin() && direction == kUp) ||
                     (startVertex.isLoop() && direction == kDown)))
                {
                    Vertex[] following = getTraversal(graphModel, siblings[i], endVertex, direction, false);
                    for (Vertex element : following) {
                        if (element == startVertex) {
                            skipBackLink = true;
                            break;
                        }
                    }
                }

                if (!skipBackLink) visitVertex(siblings[i], endVertex, graphModel, path, direction, tag, ignoreBackLinks);
            }

            //add endVertex to the result
            if (siblings[i].equals(endVertex)) path.add(endVertex);
        }
    }
}
