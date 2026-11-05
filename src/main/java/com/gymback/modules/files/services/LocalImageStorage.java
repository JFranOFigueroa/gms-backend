package com.gymback.modules.files.services;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalImageStorage {
  private static final long MAX_BYTES=8L*1024*1024;
  private final Path root;
  public LocalImageStorage(@Value("${app.files.root:/var/lib/gms/files}") String root){
    this.root=Path.of(root).toAbsolutePath().normalize();
  }
  public record Saved(String key,String mime,long size) {}
  public Saved save(UUID gym,MultipartFile file){
    if(file==null || file.isEmpty() || file.getSize()>MAX_BYTES)throw new DomainException("Imagen vacía o mayor a 8 MB");
    try {
      byte[] header=new byte[16];
      try(InputStream input=file.getInputStream()){int n=input.read(header);if(n<12)throw new DomainException("Imagen inválida");}
      String extension,mime;
      if((header[0]&255)==0x89 && header[1]==0x50 && header[2]==0x4e && header[3]==0x47 && header[4]==0x0d && header[5]==0x0a){extension="png";mime="image/png";}
      else if((header[0]&255)==0xff && (header[1]&255)==0xd8 && (header[2]&255)==0xff){extension="jpg";mime="image/jpeg";}
      else if(header[0]=='R'&&header[1]=='I'&&header[2]=='F'&&header[3]=='F'&&header[8]=='W'&&header[9]=='E'&&header[10]=='B'&&header[11]=='P'){extension="webp";mime="image/webp";}
      else throw new DomainException("Solo se permiten archivos PNG, JPG o WebP válidos");
      String key=gym+"/"+UUID.randomUUID()+"."+extension;
      Path target=resolve(key); Files.createDirectories(target.getParent());
      Path temp=Files.createTempFile(target.getParent(),"upload-",".part");
      try {
        long size;
        try(InputStream input=file.getInputStream();OutputStream out=Files.newOutputStream(temp)) {
          byte[] buffer=new byte[8192];int n;size=0;
          while((n=input.read(buffer))!=-1){size+=n;if(size>MAX_BYTES)throw new DomainException("La imagen supera 8 MB");out.write(buffer,0,n);}
        }
        try {Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);} catch(AtomicMoveNotSupportedException ex){Files.move(temp,target);}
        return new Saved(key,mime,size);
      } finally {Files.deleteIfExists(temp);}
    } catch(IOException ex){throw new IllegalStateException("No se pudo guardar imagen en almacenamiento local",ex);}
  }
  public FileSystemResource read(String key){
    Path path=resolve(key);
    if(!Files.isRegularFile(path))throw new com.gymback.shared.exceptions.NotFoundException("Imagen no disponible");
    return new FileSystemResource(path);
  }
  public void discard(String key){
    try {Files.deleteIfExists(resolve(key));}catch(IOException ignored) { /* limpiar archivos huérfanos por job */ }
  }
  private Path resolve(String key){
    if(key==null || !key.matches("[a-fA-F0-9-]{36}/[a-fA-F0-9-]{36}\\.(png|jpg|webp)"))
      throw new DomainException("Clave de archivo inválida");
    Path resolved=root.resolve(key).normalize();
    if(!resolved.startsWith(root))throw new DomainException("Ruta no permitida");
    return resolved;
  }
}
