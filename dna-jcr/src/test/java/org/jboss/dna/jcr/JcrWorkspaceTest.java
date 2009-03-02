/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.NamespaceRegistry;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrWorkspaceTest {

    private String workspaceName;
    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private RepositoryConnectionFactory connectionFactory;
    private Map<String, Object> sessionAttributes;
    @Mock
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        final String repositorySourceName = "repository";
        workspaceName = "workspace1";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(repositorySourceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();

        // Set up the initial content ...
        Graph graph = Graph.create(source, context);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and().create("/b");
        graph.set("booleanProperty").on("/a/b").to(true);
        graph.set("stringProperty").on("/a/b/c").to("value");

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return repositorySourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Stub out the repository, since we only need a few methods ...
        MockitoAnnotations.initMocks(this);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        // Now create the workspace ...
        sessionAttributes = new HashMap<String, Object>();
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowClone() throws Exception {
        workspace.clone(null, null, null, false);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCopyFromNullPathToNullPath() throws Exception {
        workspace.copy(null, null);
    }

    @Test
    public void shouldCopyFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.copy("/a/b", "/b/b-copy");
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowCopyFromOtherWorkspace() throws Exception {
        workspace.copy(null, null, null);
    }

    @Test
    public void shouldNotAllowGetAccessibleWorkspaceNames() throws Exception {
        String[] names = workspace.getAccessibleWorkspaceNames();
        assertThat(names.length, is(1));
        assertThat(names[0], is(workspaceName));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowImportContentHandlerWithNullPath() throws Exception {
        workspace.getImportContentHandler(null, 0);
    }

    @Test
    public void shouldGetImportContentHandlerWithValidPath() throws Exception {
        assertThat(workspace.getImportContentHandler("/b", 0), is(notNullValue()));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(workspace.getName(), is(workspaceName));
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {
        NamespaceRegistry registry = workspace.getNamespaceRegistry();
        assertThat(registry, is(notNullValue()));
        assertThat(registry.getURI(JcrLexicon.Namespace.PREFIX), is(JcrLexicon.Namespace.URI));
    }

    @Test
    public void shouldGetNodeTypeManager() throws Exception {
        assertThat(workspace.getNodeTypeManager(), is(notNullValue()));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetObservationManager() throws Exception {
        workspace.getObservationManager();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetQueryManager() throws Exception {
        workspace.getQueryManager();
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(workspace.getSession(), is(notNullValue()));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowImportXml() throws Exception {
        workspace.importXML(null, null, 0);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowMoveFromNullPath() throws Exception {
        workspace.move(null, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowMoveFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.move("/a/b", "/b/b-copy");
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRestore() throws Exception {
        workspace.restore(null, false);
    }
}
