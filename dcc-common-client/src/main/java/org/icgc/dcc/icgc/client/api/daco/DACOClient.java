/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.icgc.client.api.daco;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A client to query DACO API. To get a DACO access it is required to fill-in the DACO application. For more information
 * check <a href="https://icgc.org/daco">How to get DACO access</a>.<br>
 * <br>
 * DACO access is granted to OpenID accounts only. In other words, if a user has an ICGC account they also need an
 * OpenID account. However, the DACO API does not check if the account is valid or not. Thus, it's possible to use just
 * a random unique URI. Multiple ICGC accounts can share a single OpenID account. It is unknown if an ICGC account can
 * be used as the OpenID account.
 */
public interface DACOClient {

  public enum FilterType {
    OPENID("openid"),
    USERNAME("username");

    FilterType(String value) {
      this.value = value;
    }

    private String value;

    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * Gets all approved DACO users (OpenID list)
   * 
   * @see <a href="https://wiki.oicr.on.ca/pages/viewpage.action?pageId=57773218">Search - All DACO Approved Users</a>
   */
  List<User> getUsers();

  /**
   * Gets approved DACO users by {@code openId}. As multiple accounts can share the same OpenID a list of users is
   * returned
   * 
   * @param openId - OpenID being searched
   * @throws NoSuchElementException
   * @see <a href="https://wiki.oicr.on.ca/pages/viewpage.action?pageId=57773220">Search - One DACO Approved Users
   * (v2.0)</a>
   */
  List<User> getUser(String openId);

  /**
   * Returns filtered approved DACO users.
   * 
   * @param filterType - type of search (by OpenID or by username)
   * @param filterValue - OpenID or username being searched
   * @throws NoSuchElementException
   * @see <a href="https://wiki.oicr.on.ca/pages/viewpage.action?pageId=57773222">Search - One DACO Approved Users by
   * entity-filter (v2.0)</a>
   */
  List<User> getFilteredUsers(FilterType filterType, String filterValue);

  /**
   * Checks if {@code id} is in the list of the approved DACO users. The {@code id} may be either an openId or a
   * username.
   * 
   * @param id to be checked
   * @return <b>true</b> if the {@code openId} is approved, otherwise - <b>false</b>
   */
  boolean hasDacoAccess(String id, FilterType idType);

}
