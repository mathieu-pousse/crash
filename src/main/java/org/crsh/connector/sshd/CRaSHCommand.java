/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.crsh.connector.sshd;

import org.apache.sshd.common.PtyMode;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.crsh.connector.ShellConnector;
import org.crsh.shell.ShellBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class CRaSHCommand implements Command, Runnable {

  /** . */
  private final ShellBuilder builder;

  /** . */
  private InputStream in;

  /** . */
  private OutputStream out;

  /** . */
  private OutputStream err;

  /** . */
  private ExitCallback callback;

  /** . */
  private Thread thread;

  public CRaSHCommand(ShellBuilder builder) {
    this.builder = builder;
  }

  public void setInputStream(InputStream in) {
    this.in = in;
  }

  public void setOutputStream(OutputStream out) {
    this.out = out;
  }

  public void setErrorStream(OutputStream err) {
    this.err = err;
  }

  public void setExitCallback(ExitCallback callback) {
    this.callback = callback;
  }

  /** . */
  private SSHContext context;

  /** . */
  private ShellConnector connector;

  public void start(Environment env) throws IOException {

/*
    System.out.println("env.getPtyModes() = " + env.getPtyModes());
    System.out.println("env.getEnv() = " + env.getEnv());

    // 
*/
    context = new SSHContext(env.getPtyModes().get(PtyMode.VERASE));
    connector = new ShellConnector(builder);

    //
    thread = new Thread(this, "CRaSH");
    thread.start();
  }

  public void destroy() {
    connector.close();
    thread.interrupt();
  }

  public void run() {
    try {
      OutputStreamWriter writer = new OutputStreamWriter(out);
      SSHReader reader = new SSHReader(new InputStreamReader(in), context.verase, writer);

      //
      String welcome = connector.welcome();
      writer.write(welcome);
      writer.flush();

      //
      while (true) {
        String request = reader.nextLine();

        //
        if (request == null) {
          break;
        }

        //
        String response = connector.evaluate(request);

        //
        writer.write(response);
        writer.flush();

        //
        if (connector.isClosed()) {
          break;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      callback.onExit(0);
    }
  }
}

