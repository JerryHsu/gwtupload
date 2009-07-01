/*
 * Copyright 2007 Manuel Carrasco Moñino. (manuel_carrasco at users.sourceforge.net) 
 * http://code.google.com/p/gwtchismes
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

package jsupload.client;

import org.timepedia.exporter.client.Exporter;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * @author Manolo Carrasco Moñino 
 * <p>
 * This class exports the gwtUpload library into native javascript library.
 * </p>
 */
public class JsUpload implements EntryPoint {

  /**  
   * This method is called as soon as the browser loads the page.
   * Then the classes and methods are available to be used from javascript.
   * Eventually the javascript method jsuOnLoad is called if it exists.
   */
  public void onModuleLoad() {
    ((Exporter) GWT.create(Upload.class)).export();
    ((Exporter) GWT.create(PreloadImage.class)).export();
    onLoadImpl();
  }

  private native void onLoadImpl() /*-{
    if ($wnd.jsuOnLoad) $wnd.jsuOnLoad();
  }-*/;
}
