/*
 * Copyright 2009 Manuel Carrasco Moñino. (manuel_carrasco at users.sourceforge.net) 
 * http://code.google.com/p/gwtupload
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package gwtupload.server.appengine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

/**
 * <p>
 * This factory stores the data of FileItems in server's cache. 
 * </p>
 * 
 * This class in useful in App-engine where writing to file-system is not supported.
 * It has the limitation of not supporting large files due to App-engine request size 
 * is limited to 512 KB., and cache storage size to 1024 KB per object.
 * 
 * @author Manolo Carrasco Moñino
 * 
 */
public class MemCacheFileItemFactory implements FileItemFactory, Serializable {
  
  /**
   * An OutputStream that saves received data in memory.
   * 
   * When the close method is called, it invokes the save method of the object
   * which is using this. 
   *
   */
  public class CacheableByteArrayOutputStream extends OutputStream implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] buff = new byte[requestSize];
    private int size = 0;

    Saveable<CacheableByteArrayOutputStream> saveable;

    public CacheableByteArrayOutputStream(Saveable<CacheableByteArrayOutputStream> object) {
      saveable = object;
    }

    @Override
    public void close() throws IOException {
      saveable.save(this);
    }

    public byte[] get() {
      return buff;
    }

    public void reset() {
      size = 0;
    }

    public int size() {
      return size;
    }

    @Override
    public void write(int b) throws IOException {
      buff[size++] = (byte) b;
    }
  }  

  /**
   * FileItem class which stores file data in cache.
   *
   */
  public class CacheableFileItem implements FileItem, Saveable<CacheableByteArrayOutputStream> {
    String fname;
    String ctype;
    boolean formfield;
    String name;
    int size = 0;

    CacheableByteArrayOutputStream data = null;

    private static final long serialVersionUID = 1L;

    public CacheableFileItem(String fieldName, String contentType, boolean isFormField, String fileName) {
      ctype = contentType;
      fname = fieldName;
      name = fileName;
      formfield = isFormField;
      data = new CacheableByteArrayOutputStream(this);
    }

    public void delete() {
      if (data != null) {
        data.reset();
      } else {
        try {
          Cache cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
          cache.remove(fname);
        } catch (Exception e) {}
        data = new CacheableByteArrayOutputStream(this);
      }
    }

    public byte[] get() {
      return getData();
    }

    public String getContentType() {
      return ctype;
    }

    private byte[] getData() {
      if (data == null) {
        try {
          Cache cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
          return (byte[]) cache.get(fname);
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      } else {
        return data.get();
      }
    }

    public String getFieldName() {
      return fname;
    }

    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(getData());
    }

    public String getName() {
      return name;
    }

    public OutputStream getOutputStream() throws IOException {
      return data;
    }

    public long getSize() {
      return size;
    }

    public String getString() {
      return getData().toString();
    }

    public String getString(String arg0) throws UnsupportedEncodingException {
      return new String(get(), arg0);
    }

    public boolean isFormField() {
      return formfield;
    }

    public boolean isInMemory() {
      return data != null;
    }

    @SuppressWarnings({ "unchecked", "serial" })
    public void save(CacheableByteArrayOutputStream o) {
      if (data != null) {
        try {
          Cache cache = CacheManager.getInstance().getCacheFactory().createCache(new HashMap(){{
            put(GCacheFactory.EXPIRATION_DELTA, 3600); 
          }});
          cache.put(fname, data.get());
          size = data.size();
          data = null;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void setFieldName(String arg0) {
      fname = arg0;
    }

    public void setFormField(boolean arg0) {
      formfield = arg0;
    }

    public void write(File arg0) throws Exception {
      throw new UnsupportedOperationException(this.getClass().getName() + " doesn't support write to files");
    }

  }
  
  /**
   * Interface for objects that has can be saved
   *
   * @param <T>
   */
  public interface Saveable<T> {
    public void save(T o);
  }

  // Max request size in App-engine
  public static final int DEFAULT_REQUEST_SIZE = 512 * 1024;

  private static final long serialVersionUID = 1L;

  private int requestSize;
  
  public MemCacheFileItemFactory() {
    this(DEFAULT_REQUEST_SIZE);
  }

  public MemCacheFileItemFactory(int requestSize) {
    this.requestSize = requestSize;
  }

  public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
    return new CacheableFileItem(fieldName, contentType, isFormField, fileName);
  }

}
