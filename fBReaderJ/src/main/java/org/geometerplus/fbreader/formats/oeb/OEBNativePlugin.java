/*
 * Copyright (C) 2011-2014 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.formats.oeb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.amse.ys.zip.LocalFileHeader;
import org.amse.ys.zip.ZipFile;
import org.geometerplus.zlibrary.core.encodings.EncodingCollection;
import org.geometerplus.zlibrary.core.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookReadingException;
import org.geometerplus.fbreader.formats.NativeFormatPlugin;

import org.geometerplus.zlibrary.core.filesystem.ZLZipEntryFile;
import com.cnki.android.cajreader.ReaderExLib;

public class OEBNativePlugin extends NativeFormatPlugin {
	public OEBNativePlugin() {
		super("ePub");
	}

	@Override
	public void readModel(BookModel model) throws BookReadingException {
		model.Book.File.setCached(true);
		try {
			super.readModel(model);
			model.setLabelResolver(new BookModel.LabelResolver() {
				public List<String> getCandidates(String id) {
					final int index = id.indexOf("#");
					return index > 0
						? Collections.<String>singletonList(id.substring(0, index))
						: Collections.<String>emptyList();
				}
			});
		} finally {
			model.Book.File.setCached(false);
		}
	}

	@Override
	public EncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
		book.setEncoding("auto");
	}

	@Override
	public String readAnnotation(ZLFile file) {
		file.setCached(true);
		try {
			return new OEBAnnotationReader().readAnnotation(getOpfFile(file));
		} catch (BookReadingException e) {
			return null;
		} finally {
			file.setCached(false);
		}
	}

	private ZLFile getOpfFile(ZLFile oebFile) throws BookReadingException {
		if ("opf".equals(oebFile.getExtension())) {
			return oebFile;
		}

		final ZLFile containerInfoFile = ZLFile.createFile(oebFile, "META-INF/container.xml");
		if (containerInfoFile.exists()) {
			final ContainerFileReader reader = new ContainerFileReader();
			reader.readQuietly(containerInfoFile);
			final String opfPath = reader.getRootPath();
			if (opfPath != null) {
				return ZLFile.createFile(oebFile, opfPath);
			}
		}

		for (ZLFile child : oebFile.children()) {
			if (child.getExtension().equals("opf")) {
				return child;
			}
		}
		throw new BookReadingException("opfFileNotFound", oebFile);
	}

	@Override
	public int priority() {
		return 0;
	}


	private void setRightsHandle(Book book, int handle) {
		try{
	//		final ZipFile zf = book.File.getZipFile();
			final ZipFile zf = ZLZipEntryFile.getZipFile(book.File);
			if (zf != null) {
				final Collection<LocalFileHeader> headers = zf.headers();
				if (!headers.isEmpty()) {
					for (LocalFileHeader h : headers) {
						if (h.FileName.endsWith("html") ||
								h.FileName.endsWith("htm")) {
							h.Handle = handle;
						}
					}
				}
			}
		}
		catch (IOException e)
		{

		}
	}

	public boolean getRightsFile( Book book) {
		final ZLFile rightsFile = ZLFile.createFile(book.File, "META-INF/rights.xml");
		InputStream stream = null;
		try {
			stream = rightsFile.getInputStream();
			if (stream == null) {
				return false;
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte buf[] = new byte[1024];
			int numread;
			while ((numread = stream.read(buf)) != -1) {
				output.write(buf, 0, numread);
			}
			stream.close();
			byte[] rights = output.toByteArray();
			int handle = ReaderExLib.DecryptRights(rights);
			if (handle == 0)
				return false;
			book.setRightsHandle(handle);
			setRightsHandle(book, handle);

		} catch (IOException e) {
		}
		return true;
	}

	public boolean getHtmlFile( Book book) {
		final ZLFile rightsFile = ZLFile.createFile(book.File, "OEBPS/page0001.html");
		InputStream stream = null;
		try {
			stream = rightsFile.getInputStream();
			if (stream == null) {
				return false;
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte buf[] = new byte[1024];
			int numread;
			while ((numread = stream.read(buf)) != -1) {
				output.write(buf, 0, numread);
			}
			stream.close();
			byte[] rights = output.toByteArray();
			int handle = ReaderExLib.DecryptRights(rights);
			if (handle == 0)
				return false;
			book.setRightsHandle(handle);
			setRightsHandle(book, handle);

		} catch (IOException e) {
		}
		return true;
	}
}
