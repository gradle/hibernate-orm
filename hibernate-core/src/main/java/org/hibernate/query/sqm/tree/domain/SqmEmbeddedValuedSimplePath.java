/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath extends AbstractSqmSimplePath {
	public SqmEmbeddedValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			EmbeddedValuedNavigable referencedNavigable,
			SqmPath lhs) {
		super( uid, navigablePath, referencedNavigable, lhs );
	}

	public SqmEmbeddedValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			EmbeddedValuedNavigable referencedNavigable,
			SqmPath lhs, String explicitAlias) {
		super( uid, navigablePath, referencedNavigable, lhs, explicitAlias );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final Navigable subNavigable = getReferencedNavigable().findNavigable( name );
		final NavigablePath subNavigablePath = getNavigablePath().append( name );

		getLhs().prepareForSubNavigableReference(
				this,
				false,
				creationState
		);

		return creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
				subNavigablePath,
				snp -> {
					// Create the join for the embeddable
					final SqmFrom embedJoinLhs = creationState.getProcessingStateStack()
							.getCurrent()
							.getPathRegistry()
							.findFromByPath( getLhs().getNavigablePath() );

					final SqmNavigableJoin join = getReferencedNavigable().createJoin(
							embedJoinLhs,
							// implicit joins are always INNER
							SqmJoinType.INNER,
							null,
							false,
							creationState
					);
					embedJoinLhs.addJoin( join );

					creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( join );

					return subNavigable.createSqmExpression(
							join,
							creationState
					);
				}
		);
	}

	@Override
	public EmbeddedValuedNavigable getReferencedNavigable() {
		return (EmbeddedValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public EmbeddedValuedNavigable getExpressableType() {
		return (EmbeddedValuedNavigable) super.getExpressableType();
	}

	@Override
	public Supplier<? extends EmbeddedValuedNavigable> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEmbeddableValuedPath( this );
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			SqmPath subReference,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do
			return;
		}

		SqmCreationHelper.resolveAsLhs( getLhs(), this, subReference, isSubReferenceTerminal, creationState );
		dereferenced = true;
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CompositeResultImpl( getNavigablePath(), getReferencedNavigable(), resultVariable, creationState );
//	}
}