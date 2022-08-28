export enum LogLevel {OFF=0, ERROR=1, WARNING=2, INFO=3, DEBUG=4, TRACE=5};

export class Logger {

  private prefix = ""
  private configLevel = LogLevel.DEBUG;

  constructor(pckg: string, clazz: string) {
    this.prefix = pckg + "." + clazz;
  }

  public error(...args: any[]): void {
    this.log(LogLevel.ERROR, args)
  }

  public warn(...args: any[]): void {
    this.log(LogLevel.WARNING, args)
  }

  public info(...args: any[]): void {
    this.log(LogLevel.INFO, args)
  }

  public debug(...args: any[]): void {
    this.log(LogLevel.DEBUG, args)
  }

  public trace(...args: any[]): void {
    this.log(LogLevel.TRACE, args)
  }

  private log(msgLevel: LogLevel, ...args: any[]): void {
    if (this.configLevel < msgLevel) {
      return
    }

    /* TODO: make this work
    // Extend prefix with method name if it was provided in the first paramater
    if (typeof args[0] === 'string' && args[0].toString().endsWith('()')) {
      const method: string = args.shift();
      this.prefix += '.' + method;
    }

    // eslint-disable-next-line no-restricted-syntax
    console.debug('Logger', 'prefix:', this.prefix, 'configLevel:', this.configLevel, 'msgLevel:', msgLevel)
    */

    if (msgLevel === LogLevel.ERROR) {
      console.error(this.prefix, ...args);
    }
    else if (msgLevel === LogLevel.WARNING) {
      console.warn(this.prefix, ...args);
    }
    else if (msgLevel === LogLevel.INFO) {
      console.log(this.prefix, ...args);
    }
    else if (msgLevel === LogLevel.DEBUG) {
      // eslint-disable-next-line no-restricted-syntax
      console.debug(this.prefix, ...args);
    }
    else if (msgLevel === LogLevel.TRACE) {
      // eslint-disable-next-line no-restricted-syntax
      console.trace(this.prefix, ...args);
    }
  }
}
