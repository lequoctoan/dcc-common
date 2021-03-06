/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.common.core.meta;

import static org.icgc.dcc.common.core.dcc.DccResources.DICTIONARY_VERSION;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.icgc.dcc.common.core.util.Optionals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

public abstract class AbstractArtifactoryResolver implements Resolver {

  private static String getDefaultVersion() {
    return DICTIONARY_VERSION;
  }

  protected static <T> T read(String fileName, Class<T> type) {
    return read(fileName, type, Optional.of(getDefaultVersion()));
  }

  @SneakyThrows
  protected static <T> T read(String fileName, Class<T> type, Optional<String> version) {
    // Resolve
    @Cleanup
    val zip = new ZipInputStream(getUrl(version).openStream());
    ZipEntry entry;

    val entryName = "org/icgc/dcc/resources/" + fileName;
    do {
      entry = zip.getNextEntry();
    } while (!entryName.equals(entry.getName()));

    return new ObjectMapper().readValue(zip, type);
  }

  protected static URL getUrl(Optional<String> optionalVersion) throws MalformedURLException {
    val basePath = "https://artifacts.oicr.on.ca/artifactory";
    val template = "%s/simple/dcc-dependencies/org/icgc/dcc/dcc-resources/%s/dcc-resources-%s.jar";
    val version = Optionals.defaultValue(optionalVersion, getDefaultVersion());
    URL url = new URL(String.format(template, basePath, version, version));

    return url;
  }

}
