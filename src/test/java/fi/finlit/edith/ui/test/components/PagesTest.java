package fi.finlit.edith.ui.test.components;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.internal.services.MarkupWriterImpl;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.finlit.edith.EdithTestConstants;
import fi.finlit.edith.sql.domain.Document;
import fi.finlit.edith.ui.components.document.Pages;
import fi.finlit.edith.ui.services.DocumentDao;
import fi.finlit.edith.ui.services.hibernate.AbstractHibernateTest;
import fi.finlit.edith.ui.services.svn.SubversionService;

public class PagesTest extends AbstractHibernateTest {

    @Inject
    @Symbol(EdithTestConstants.TEST_DOCUMENT_KEY)
    private String testDocument;

    @Inject
    private DocumentDao documentDao;

    @Autobuild
    @Inject
    private Pages pages;

    @Inject
    private SubversionService subversionService;

    @Before
    public void before() {
        subversionService.initialize();
    }

    @After
    public void after() {
        subversionService.destroy();
    }

    @Test
    public void BeginRender() throws XMLStreamException, IOException{
        Document document = documentDao.getDocumentForPath(testDocument);
        pages.setDocument(document);

        MarkupWriter writer = new MarkupWriterImpl();
        pages.beginRender(writer);
        assertTrue(writer.toString().startsWith("<ul "));
        assertTrue(writer.toString().endsWith("</ul>"));
    }

}
