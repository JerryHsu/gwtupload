package gwtupload.server;



import gwtupload.server.appengine.MemCacheFileItemFactory;
import gwtupload.server.exceptions.UploadCanceledException;
import gwtupload.server.exceptions.UploadSizeLimitException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

public class TestUpload {
  

  private void emulateUpload(final AbstractUploadListener listener) {
    Thread t = new Thread() {
      public void run() {
          listener.update(0, 100, 1);
          msleep(500);
          listener.update(50, 100, 1);
          msleep(500);
          listener.update(100, 100, 1);
      }
    };
    t.start();
  }
  
  private void msleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception e) {
    }
  }
  
  public <T> T serializeAndDeserialize(T object) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream fout = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(fout);
    oos.writeObject(object);
    oos.close();

    ByteArrayInputStream fin = new ByteArrayInputStream(fout.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(fin);
    @SuppressWarnings("unchecked")
    T objectB = (T)ois.readObject();
    ois.close();

    return objectB;
  }

  @Test
  public void testExceptionsHasToBeSerializable() throws IOException, ClassNotFoundException {

    UploadSizeLimitException usleA = new UploadSizeLimitException(50000, 10000);
    UploadSizeLimitException usleB = serializeAndDeserialize(usleA);
    Assert.assertNotNull(usleB);
    Assert.assertEquals(usleA.getMessage(), usleB.getMessage());
    
  }
  
  @Test
  public void testMemCacheItemFileItemHasToBeSerializable() throws IOException, ClassNotFoundException {

    MemCacheFileItemFactory fItemFact = new MemCacheFileItemFactory();
    FileItem itemA = fItemFact.createItem("name", "text", false, "pp.jpg");
    OutputStream o = itemA.getOutputStream();
    o.write(new byte[] { 1, 2, 3, 4, 5, 6, 5, 6, 7, 8, 9, 0 });
    
    FileItem itemB = serializeAndDeserialize(itemA);

    Assert.assertNotNull(itemB);
    Assert.assertEquals(itemA.getSize(), itemB.getSize());
  }
  
  @Test
  public void testMemoryUploadListener() throws Exception {
    HttpServletRequest request = new MockHttpRequest();
    UploadServlet.setThreadLocalRequest(request);
    
    Assert.assertNotNull(UploadServlet.getThreadLocalRequest());
    Assert.assertNotNull(UploadServlet.getThreadLocalRequest().getSession());
    Assert.assertNotNull(UploadServlet.getThreadLocalRequest().getSession().getId());
    String sessionId = UploadServlet.getThreadLocalRequest().getSession().getId();
    AbstractUploadListener l1 = new MemoryUploadListener(0, 0);
    emulateUpload(l1);
    
    AbstractUploadListener l2 = MemoryUploadListener.current(sessionId);
    Assert.assertEquals(0, l2.getBytesRead());
    Assert.assertEquals(100, l2.getContentLength());
    msleep(501);
    Assert.assertEquals(50, l2.getBytesRead());
    msleep(501);
    Assert.assertEquals(100, l2.getBytesRead());
    l1.remove();
    Assert.assertNull(MemoryUploadListener.current(sessionId));
  }
  
  @Test
  public void testUploadListenerHasToBeSerializable() throws IOException, ClassNotFoundException {

    AbstractUploadListener listenerA = new UploadListener(300, 100);
    listenerA.update(80, 100, 1);
    
    AbstractUploadListener listenerB = serializeAndDeserialize(listenerA);
    
    Assert.assertNotNull(listenerB);
    Assert.assertEquals(80, listenerB.getPercent());
    
    listenerA.setException(new UploadCanceledException());
    listenerB = serializeAndDeserialize(listenerA);
    
    Assert.assertNotNull(listenerB.getException());
  }

}