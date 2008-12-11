/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nodebox.client;

import java.util.ArrayList;
import java.util.List;

public class Application {

    private static Application instance = new Application();

    public static Application getInstance() {
        return instance;
    }

    private List<Document> documents = new ArrayList<Document>();
    private Document currentDocument;

    private Application() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        createNewDocument();
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public int getDocumentCount() {
        return documents.size();
    }

    public void removeDocument(Document document) {
        documents.remove(document);
    }

    public Document createNewDocument() {
        Document doc = new Document();
        doc.setVisible(true);
        documents.add(doc);
        currentDocument = doc;
        return doc;
    }

    public static void main(String[] args) {
        // new Application();
    }
}
