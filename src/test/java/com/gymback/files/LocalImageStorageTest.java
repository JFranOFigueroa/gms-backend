package com.gymback.files;
import com.gymback.modules.files.services.LocalImageStorage;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class LocalImageStorageTest {
 @TempDir Path temporary;
 @Test void savesPngUnderTenantDirectoryAndPreventsTraversal() throws Exception {
   var storage=new LocalImageStorage(temporary.toString());
   var gym=UUID.randomUUID();
   byte[] png=new byte[]{(byte)0x89,'P','N','G',13,10,26,10,0,0,0,0,0,0,0,0};
   var saved=storage.save(gym,new MockMultipartFile("file","../escape.png","image/png",png));
   assertTrue(saved.key().startsWith(gym+"/"));
   assertTrue(Files.isRegularFile(temporary.resolve(saved.key())));
   assertEquals("image/png",saved.mime());
   storage.discard(saved.key());
   assertFalse(Files.exists(temporary.resolve(saved.key())));
   assertThrows(DomainException.class,()->storage.read("../../etc/passwd"));
 }
 @Test void rejectsUnknownBinary(){
   var storage=new LocalImageStorage(temporary.toString());
   byte[] content="not an image at all".getBytes(java.nio.charset.StandardCharsets.UTF_8);
   assertThrows(DomainException.class,()->storage.save(UUID.randomUUID(),new MockMultipartFile("file","x.bin","application/octet-stream",content)));
 }
}
