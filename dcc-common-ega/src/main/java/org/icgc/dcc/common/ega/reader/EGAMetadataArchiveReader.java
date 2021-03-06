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
package org.icgc.dcc.common.ega.reader;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.common.core.util.Formats.formatCount;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.icgc.dcc.common.core.io.ForwardingInputStream;
import org.icgc.dcc.common.ega.model.EGAMetadataArchive;
import org.icgc.dcc.common.ega.utils.XMLObjectNodeReader;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads a remote EGA metadata tarball into a {@link EGAMetadataArchive}.
 */
@Slf4j
@RequiredArgsConstructor
public class EGAMetadataArchiveReader {

  /**
   * Constants.
   */
  public static final String DEFAULT_API_URL = "http://ega.ebi.ac.uk/ega/rest/download/v2";

  private static final int MAX_ATTEMPTS = 20;
  private static final int READ_TIMEOUT = (int) SECONDS.toMillis(5);

  private static final XMLObjectNodeReader XML_READER = new XMLObjectNodeReader();
  private static final EGAMappingReader MAPPING_READER = new EGAMappingReader();

  /**
   * Configuration.
   */
  @NonNull
  private final String apiUrl;

  public EGAMetadataArchiveReader() {
    this(DEFAULT_API_URL);
  }

  @SneakyThrows
  public EGAMetadataArchive read(@NonNull String datasetId) {
    @Cleanup
    val tarball = readTarball(datasetId);

    TarArchiveEntry entry = null;
    val archive = new EGAMetadataArchive(datasetId);
    while ((entry = tarball.getNextTarEntry()) != null) {
      try {
        if (isMappingFile(entry)) {
          readMappingEntry(tarball, entry, archive);
        } else if (isXmlFile(entry)) {
          readXmlEntry(tarball, entry, archive);
        }
      } catch (Exception e) {
        val message = MessageFormat.format("Error processing entry {0} from {1} after reading {2} bytes",
            entry.getName(), getArchiveUrl(datasetId), formatCount(tarball.getBytesRead()));
        throw new IllegalStateException(message, e);
      }
    }

    return archive;
  }

  private static void readMappingEntry(TarArchiveInputStream tarball, TarArchiveEntry entry,
      EGAMetadataArchive archive) {
    val mappingId = getMappingId(entry);
    val mapping = parseMapping(mappingId, tarball);

    archive.getMappings().put(mappingId, mapping);
  }

  private static void readXmlEntry(TarArchiveInputStream tarball, TarArchiveEntry entry, EGAMetadataArchive archive) {
    val xml = parseXml(tarball);

    if (isStudy(entry)) {
      archive.getStudies().put(getStudyId(entry), xml);
    } else if (isSample(entry)) {
      archive.getSamples().put(getSampleId(entry), xml);
    } else if (isExperiment(entry)) {
      archive.getExperiments().put(getExperimentId(entry), xml);
    } else if (isRun(entry)) {
      archive.getRuns().put(getRunId(entry), xml);
    } else if (isAnalysis(entry)) {
      archive.getAnalysis().put(getAnalysisId(entry), xml);
    }
  }

  private static ObjectNode parseXml(InputStream inputStream) {
    return XML_READER.read(new ForwardingInputStream(inputStream, false));
  }

  private static List<ObjectNode> parseMapping(String mappingId, InputStream inputStream) {
    // Prevent close by wrapping with non-closing forwarding stream
    return MAPPING_READER.read(mappingId, new ForwardingInputStream(inputStream, false));
  }

  private TarArchiveInputStream readTarball(String datasetId) throws IOException {
    int attempts = 0;
    val url = getArchiveUrl(datasetId);
    while (++attempts <= MAX_ATTEMPTS) {
      try {
        val connection = url.openConnection();
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(READ_TIMEOUT);

        val gzip = new GZIPInputStream(connection.getInputStream());
        return new TarArchiveInputStream(gzip);
      } catch (SocketTimeoutException e) {
        log.warn("*** Attempt [{}/{}] failed: Socket timeout for {} after {} attempt(s)",
            attempts, MAX_ATTEMPTS, datasetId, attempts);
      } catch (IOException e) {
        log.warn("*** Attempt [{}/{}] failed: Error reading tarball for dataset {} from {}: {}",
            attempts, MAX_ATTEMPTS, datasetId, url, e.getMessage());
      }
    }

    throw new IllegalStateException("Could not read " + datasetId + " from " + url);
  }

  @SneakyThrows
  private URL getArchiveUrl(String datasetId) {
    return new URL(apiUrl + "/metadata/" + datasetId);
  }

  private static String getMappingId(TarArchiveEntry entry) {
    return getId(entry, ".map");
  }

  private static String getStudyId(TarArchiveEntry entry) {
    return getId(entry, ".study.xml");
  }

  private static String getSampleId(TarArchiveEntry entry) {
    return getId(entry, ".sample.xml");
  }

  private static String getExperimentId(TarArchiveEntry entry) {
    return getId(entry, ".experiment.xml");
  }

  private static String getRunId(TarArchiveEntry entry) {
    return getId(entry, ".run.xml");
  }

  private static String getAnalysisId(TarArchiveEntry entry) {
    return getId(entry, ".analysis.xml");
  }

  private static String getId(TarArchiveEntry entry, String suffix) {
    return new File(entry.getName()).getName().replace(suffix, "");
  }

  private static boolean isMappingFile(TarArchiveEntry entry) {
    return entry.isFile() && entry.getName().toLowerCase().endsWith(".map");
  }

  private static boolean isStudy(TarArchiveEntry entry) {
    return is(entry, "/xmls/study/");
  }

  private static boolean isSample(TarArchiveEntry entry) {
    return is(entry, "/xmls/samples/");
  }

  private static boolean isExperiment(TarArchiveEntry entry) {
    return is(entry, "/xmls/experiments/");
  }

  private static boolean isRun(TarArchiveEntry entry) {
    return is(entry, "/xmls/runs/");
  }

  private static boolean isAnalysis(TarArchiveEntry entry) {
    return is(entry, "/xmls/analysis/");
  }

  private static boolean is(TarArchiveEntry entry, String path) {
    return entry.getName().contains(path);
  }

  private static boolean isXmlFile(TarArchiveEntry entry) {
    return entry.isFile() && entry.getName().toLowerCase().endsWith(".xml");
  }

}
