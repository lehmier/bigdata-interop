/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.hadoop.gcsio;

import com.google.common.base.Strings;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Contains information about a file or a directory.
 *
 * Note:
 * This class wraps GoogleCloudStorageItemInfo, adds file system specific information and hides
 * bucket/object specific information. The wrapped type should not be visible to callers of
 * GoogleCloudStorageFileSystem because it exposes non-file system information (eg, buckets).
 */
public class FileInfo {
  // Info about the root path.
  public static final FileInfo ROOT_INFO = new FileInfo(GoogleCloudStorageItemInfo.ROOT_INFO);

  // Path of this file or directory.
  private final URI path;

  // Information about the underlying GCS item.
  private final GoogleCloudStorageItemInfo itemInfo;

  /**
   * Constructs an instance of FileInfo.
   *
   * @param itemInfo Information about the underlying item.
   */
  private FileInfo(GoogleCloudStorageItemInfo itemInfo) {
    this.itemInfo = itemInfo;

    // Construct the path once.
    this.path = GoogleCloudStorageFileSystem.getPath(
        itemInfo.getBucketName(), itemInfo.getObjectName(), true);
  }

  /**
   * Gets the path of this file or directory.
   */
  public URI getPath() {
    return path;
  }

  /**
   * Indicates whether this item is a directory.
   */
  public boolean isDirectory() {
    return isDirectory(itemInfo);
  }

  /**
   * Indicates whether {@code itemInfo} is a directory; static version of {@link #isDirectory()}
   * to avoid having to create a FileInfo object just to use this logic.
   */
  static boolean isDirectory(GoogleCloudStorageItemInfo itemInfo) {
    return isGlobalRoot(itemInfo) || itemInfo.isBucket() ||
        objectHasDirectoryPath(itemInfo.getObjectName());
  }

  /**
   * Indicates whether this instance has information about the unique, shared root of the
   * underlying storage system.
   */
  public boolean isGlobalRoot() {
    return isGlobalRoot(itemInfo);
  }

  /**
   * Static version of {@link #isGlobalRoot()} to allow sharing this logic without creating
   * unnecessary FileInfo instances.
   */
  static boolean isGlobalRoot(GoogleCloudStorageItemInfo itemInfo) {
    return itemInfo.isRoot()
        && itemInfo.exists();
  }

  /**
   * Gets creation time of this item.
   *
   * Time is expressed as milliseconds since January 1, 1970 UTC.
   */
  public long getCreationTime() {
    return itemInfo.getCreationTime();
  }

  /**
   * Gets the size of this file or directory.
   *
   * For files, size is in number of bytes.
   * For directories size is 0.
   * For items that do not exist, size is -1.
   */
  public long getSize() {
    return itemInfo.getSize();
  }

  /**
   * Indicates whether this file or directory exists.
   */
  public boolean exists() {
    return itemInfo.exists();
  }

  /**
   * Gets string representation of this instance.
   */
  public String toString() {
    if (exists()) {
      return String.format("%s: created on: %s",
          getPath(), (new Date(getCreationTime())).toString());
    } else {
      return String.format("%s: exists: no", getPath());
    }
  }

  /**
   * Gets information about the underlying item.
   */
  public GoogleCloudStorageItemInfo getItemInfo() {
    return itemInfo;
  }

  /**
   * Indicates whether the given object name looks like a directory path.
   *
   * @param objectName Name of the object to inspect.
   * @return Whether the given object name looks like a directory path.
   */
  static boolean objectHasDirectoryPath(String objectName) {
    return !Strings.isNullOrEmpty(objectName) &&
        objectName.endsWith(GoogleCloudStorage.PATH_DELIMITER);
  }

  /**
   * Converts the given object name to look like a directory path.
   * If the object name already looks like a directory path then
   * this call is a no-op.
   *
   * If the object name is null or empty, it is returned as-is.
   *
   * @param objectName Name of the object to inspect.
   * @return Directory path for the given path.
   */
  static String convertToDirectoryPath(String objectName) {
    if (!Strings.isNullOrEmpty(objectName)) {
      if (!objectHasDirectoryPath(objectName)) {
        objectName += GoogleCloudStorage.PATH_DELIMITER;
      }
    }
    return objectName;
  }

  /**
   * Converts the given object name to look like a file path.
   * If the object name already looks like a file path then
   * this call is a no-op.
   *
   * If the object name is null or empty, it is returned as-is.
   *
   * @param objectName Name of the object to inspect.
   * @return File path for the given path.
   */
  public static String convertToFilePath(String objectName) {
    if (!Strings.isNullOrEmpty(objectName)) {
      if (objectHasDirectoryPath(objectName)) {
        objectName = objectName.substring(0, objectName.length() - 1);
      }
    }
    return objectName;
  }

  /**
   * Handy factory method for constructing a FileInfo from a GoogleCloudStorageItemInfo while
   * potentially returning a singleton instead of really constructing an object for cases like ROOT.
   */
  public static FileInfo fromItemInfo(GoogleCloudStorageItemInfo itemInfo) {
    if (itemInfo.isRoot()) {
      return ROOT_INFO;
    } else {
      return new FileInfo(itemInfo);
    }
  }

  /**
   * Handy factory method for constructing a list of FileInfo from a list of
   * GoogleCloudStorageItemInfo.
   */
  public static List<FileInfo> fromItemInfos(List<GoogleCloudStorageItemInfo> itemInfos) {
    List<FileInfo> fileInfos = new ArrayList<>();
    for (GoogleCloudStorageItemInfo itemInfo : itemInfos) {
      fileInfos.add(fromItemInfo(itemInfo));
    }
    return fileInfos;
  }

  /**
   * Indicates whether the given path looks like a directory path.
   *
   * @param path Path to inspect.
   * @return Whether the path looks like a directory path.
   */
  public static boolean isDirectoryPath(URI path) {
    return (path != null) &&
        path.toString().endsWith(GoogleCloudStorage.PATH_DELIMITER);
  }

  /**
   * Converts the given resourceId to look like a directory path.
   * If the path already looks like a directory path then
   * this call is a no-op.
   *
   * @param resourceId StorageResourceId to convert.
   * @return A resourceId with a directory path corresponding to the given resourceId.
   */
  public static StorageResourceId convertToDirectoryPath(StorageResourceId resourceId) {
    if (resourceId.isStorageObject()) {
      if (!objectHasDirectoryPath(resourceId.getObjectName())) {
        resourceId = new StorageResourceId(
            resourceId.getBucketName(), convertToDirectoryPath(resourceId.getObjectName()));
      }
    }
    return resourceId;
  }

  /**
   * Converts the given path to look like a directory path.
   * If the path already looks like a directory path then
   * this call is a no-op.
   *
   * @param path Path to convert.
   * @return Directory path for the given path.
   */
  public static URI convertToDirectoryPath(URI path) {
    StorageResourceId resourceId = GoogleCloudStorageFileSystem.validatePathAndGetId(path, true);

    if (resourceId.isStorageObject()) {
      if (!objectHasDirectoryPath(resourceId.getObjectName())) {
        resourceId = convertToDirectoryPath(resourceId);
        path = GoogleCloudStorageFileSystem.getPath(
            resourceId.getBucketName(), resourceId.getObjectName());
      }
    }
    return path;
  }
}