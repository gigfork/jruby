/*
 * UntilNode.java - No description
 * Created on 05. November 2001, 21:46
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.ast;

import org.ablaf.ast.INode;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;

/** Represents an until statement.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class UntilNode extends AbstractNode {
    static final long serialVersionUID = -2929327250252365636L;

    private final INode conditionNode;
    private final INode bodyNode;

    public UntilNode(ISourcePosition position, INode conditionNode, INode bodyNode) {
        super(position);
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitUntilNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the conditionNode.
     * @return Returns a INode
     */
    public INode getConditionNode() {
        return conditionNode;
    }
}
