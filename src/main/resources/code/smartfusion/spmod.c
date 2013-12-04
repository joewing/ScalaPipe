/*
 * spmod.c - ScalaPipe interface
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/fs.h>
#include <asm/uaccess.h>
#include <asm/io.h>
#include <linux/ioport.h>

#define COMMAND_INDEX      0
#define PORT_INDEX         1
#define COUNT_INDEX        2
#define DATA_INDEX         3
#define INDEX_COUNT        4

#define BASE_ADDR       0x40054000
#define END_ADDR        (BASE_ADDR + INDEX_COUNT * 4)

#define START_COMMAND   1
#define STOP_COMMAND    2

/* Device major number */
static uint spmod_major = 167;
module_param(spmod_major, uint, S_IRUSR | S_IWUSR);
MODULE_PARM_DESC(spmod_major, "driver major number");

/* Device name */
static const char *spmod_name = "spmod";

/* Device access lock. */
static int spmod_lock = 0;

static int *base_ptr = NULL;

/** Device open */
static int spmod_open(struct inode *inode, struct file *file)
{

   /* One process at a time */
   if (spmod_lock++ > 0) {
      return -EBUSY;
   }

   /* Increment the module use counter */
   try_module_get(THIS_MODULE);

   /* Start the device. */
   iowrite32(START_COMMAND, &base_ptr[COMMAND_INDEX]);

   return 0;

}

/* Device close */
static int spmod_release(struct inode *inode, struct file *file)
{
   iowrite32(STOP_COMMAND, &base_ptr[COMMAND_INDEX]);
   spmod_lock = 0;
   module_put(THIS_MODULE);
   return 0;
}

/* Device read */
static ssize_t spmod_read(struct file *filp, char *buffer,
                          size_t length, loff_t *offset)
{
   unsigned int len;
   unsigned int i;
   unsigned int count;
   int *dest;

   /* Validate the user buffer. */
   if(!access_ok(0, buffer, length)) {
      return -EINVAL;
   }

   /* Make sure the user buffer is aligned. */
   if(((int)buffer & 3) || (length & 3)) {
      return -EINVAL;
   }

   /* Determine the max amount we can read.
    * Note that this value is reported in 4-byte units.
    */
   len = ioread32(&base_ptr[COUNT_INDEX]) << 2;

   /* Determine how much to read (reads must be a multiple of 4). */
   if(len > length) {
      len = length;
   }
   count = len >> 2;

   /* Read the data. */
   dest = (int*)buffer;
   for(i = 0; i < count; i++) {
      dest[i] = ioread32(&base_ptr[DATA_INDEX]);
   }

   *offset += len;

   return len;

}

/* Device write */
static ssize_t spmod_write(struct file *filp, const char *buffer,
                           size_t length, loff_t *offset)
{

   unsigned int len;
   unsigned int count;
   unsigned int i;
   const int *src;

   /* Validate the user buffer. */
   if(!access_ok(0, buffer, length)) {
      return -EINVAL;
   }

   /* Make sure the user buffer is aligned. */
   if(((int)buffer & 3) || (length & 3)) {
      return -EINVAL;
   }

   /* Determine the max size we can write.
    * Note that this is in 4-byte units. */
   len = ioread32(&base_ptr[COUNT_INDEX]) << 2;

   /* Determine how much to write (writes must be a multiple of 4). */
   if(len > length) {
      len = length;
   }
   count = len >> 2;

   /* Write the data. */
   src = (const int*)buffer;
   for(i = 0; i < count; i++) {
      iowrite32(src[i], &base_ptr[DATA_INDEX]);
   }

   *offset += len;

   return len;

}

static int spmod_ioctl(struct inode *inode,
                       struct file *file,
                       unsigned int ioctl_num,
                       unsigned long ioctl_param)
{
   if(ioctl_num == 0) {
      /* Set the port number. */
      iowrite32(ioctl_param, &base_ptr[PORT_INDEX]);
   }
   return 0;
}

/* Device operations */
static struct file_operations spmod_fops = {
   .read = spmod_read,
   .write = spmod_write,
   .ioctl = spmod_ioctl,
   .open = spmod_open,
   .release = spmod_release
};

static int __init spmod_init_module(void)
{
   int ret;

   /* Check that the user has supplied a correct major number. */
   if(spmod_major == 0) {
      printk(KERN_ALERT "%s: spmod_major can't be 0\n", __func__);
      return -EINVAL;
   }

   /* Register device */
   ret = register_chrdev(spmod_major, spmod_name, &spmod_fops);
   if(ret < 0) {
      printk(KERN_ALERT "%s: registering device %s with major %d "
             "failed with %d\n",
             __func__, spmod_name, spmod_major, ret);
      return ret;
   }

   /* Map the memory region. */
   if(check_mem_region(BASE_ADDR, END_ADDR - BASE_ADDR)) {
      printk(KERN_ALERT "%s: could not map memory region for %s",
             __func__, spmod_name);
      return -EBUSY;
   }
   request_mem_region(BASE_ADDR, END_ADDR - BASE_ADDR, "spmod");

   base_ptr = ioremap(BASE_ADDR, 4 * INDEX_COUNT);

   return 0;

}

static void __exit spmod_cleanup_module(void)
{
   unregister_chrdev(spmod_major, spmod_name);
   iounmap(base_ptr);
   release_mem_region(BASE_ADDR, END_ADDR - BASE_ADDR);
}

module_init(spmod_init_module);
module_exit(spmod_cleanup_module);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Joe Wingbermuehle");
MODULE_DESCRIPTION("ScalaPipe device driver");

