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
package org.icgc.dcc.common.ega.core;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.NoArgsConstructor;

/**
 * Accessors for working with EGA run files.
 */
@NoArgsConstructor(access = PRIVATE)
public final class EGARuns {

  public static JsonNode getRun(JsonNode root) {
    return root.path("RUN_SET").path("RUN");
  }

  public static String getRunDate(JsonNode root) {
    return root.path("RUN_SET").path("RUN").path("run_date").textValue();
  }

  public static JsonNode getRunFile(JsonNode root) {
    return getRun(root).path("DATA_BLOCK").path("FILES").path("FILE");
  }

  public static String getRunFileType(JsonNode file) {
    return file.path("filetype").textValue();
  }

  public static String getRunFileName(JsonNode file) {
    return file.path("filename").textValue();
  }

  public static String getRunChecksum(JsonNode file) {
    return file.path("checksum").textValue();
  }

}
